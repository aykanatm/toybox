package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.schema.actuator.Health;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Component
public class LoadbalancerUtils {
    private static final Log _logger = LogFactory.getLog(LoadbalancerUtils.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;

    @Autowired
    private DiscoveryClient discoveryClient;

    @LogEntryExitExecutionTime
    public String getLoadbalancerUrl(String loadbalancerServiceName, String fallbackServiceName, HttpSession session, boolean fastFallback) {
        List<ServiceInstance> instances = discoveryClient.getInstances(loadbalancerServiceName);
        // If there is at least one load balancer available and we don't want to check the fallback service name directly
        if(!instances.isEmpty() && !fastFallback){
            for(ServiceInstance availableInstance: instances){
                String loadbalancerUrl =  availableInstance.getUri().toString();
                String loadbalancerStatus = getLoadbalancerStatus(loadbalancerUrl, session);
                _logger.debug("[" + loadbalancerServiceName + "] Load balancer '" + loadbalancerUrl + "' is '" + loadbalancerStatus + "'");
                if(loadbalancerStatus.equalsIgnoreCase(Status.UP.getCode())){
                    _logger.debug("Load balancer URL: " + loadbalancerUrl);
                    return availableInstance.getUri().toString();
                }
            }

            _logger.warn("There is no instance that is UP with name '" + loadbalancerServiceName + "'. Trying to fall back to '" + fallbackServiceName + "'...");
            return getLoadbalancerUrl(loadbalancerServiceName, fallbackServiceName, session, true);
        }
        // If there is no load balancers available or if we already know there is no load balancers available
        else{
            _logger.warn("There is no load balancer instance with name '" + loadbalancerServiceName + "'. Trying to fall back to '" + fallbackServiceName + "'...");
            List<ServiceInstance> fallbackInstances = discoveryClient.getInstances(fallbackServiceName);
            if(!fallbackInstances.isEmpty()){
                for(ServiceInstance availableFallbackInstance: fallbackInstances){
                    String fallbackUrl = availableFallbackInstance.getUri().toString();
                    String loadbalancerStatus = getLoadbalancerStatus(fallbackUrl, session);
                    _logger.debug("[" + fallbackServiceName + "] Load balancer '" + fallbackUrl + "' is '" + loadbalancerStatus + "'");
                    if (loadbalancerStatus.equalsIgnoreCase(Status.UP.getCode())) {
                        _logger.debug("Fallback URL: " + fallbackUrl);
                        return availableFallbackInstance.getUri().toString();
                    }
                }

                throw new IllegalArgumentException("There is no fallback instance that is UP with name '" + fallbackServiceName + "'.");
            }
            else{
                throw new IllegalArgumentException("There is no fallback instance with name '" + fallbackServiceName + "'.");
            }
        }
    }

    @LogEntryExitExecutionTime
    public String getPrefix(String serviceName) throws Exception {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if(!instances.isEmpty()){
            List<Boolean> serviceSecurity = new ArrayList<>();
            for(ServiceInstance serviceInstance: instances){
                serviceSecurity.add(serviceInstance.isSecure());
            }

            boolean result = serviceSecurity.get(0);

            for(boolean isServiceSecure : serviceSecurity){
                result ^= isServiceSecure;
            }

            if(!result){
                return result ? "https://" : "http://";
            }
            else{
                throw new Exception("Not all " + serviceName + " services have the same transfer protocol!");
            }
        }
        else{
            throw new IllegalArgumentException("There is no instance with name '" + serviceName + "'!");
        }
    }

    @LogEntryExitExecutionTime
    private String getLoadbalancerStatus(String url, HttpSession session){
        try{
            HttpHeaders headers = authenticationUtils.getHeaders(session);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Health> exchange = restTemplate.exchange(url + "/health", HttpMethod.GET, new HttpEntity<>(headers), Health.class);
            Health responseBody = exchange.getBody();
            return responseBody.getStatus();
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            _logger.error("Health endpoint of url '" + url + "' returned an error response. " + responseJson.get("message").getAsString());
            return Status.UNKNOWN.getCode();
        }
        catch (ResourceAccessException rae){
            _logger.error("The loadbalancer at '" + url + "' is not reachable. " + rae.getLocalizedMessage());
            return Status.UNKNOWN.getCode();
        }
        catch (Exception e){
            _logger.error("An error occurred while trying to check the health of the the loadbalancer at '" + url + "'. " + e.getLocalizedMessage());
            return Status.UNKNOWN.getCode();
        }
    }
}