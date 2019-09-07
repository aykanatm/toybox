package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LoadbalancerUtils {
    private static final Log _logger = LogFactory.getLog(LoadbalancerUtils.class);

    @Autowired
    private DiscoveryClient discoveryClient;

    @LogEntryExitExecutionTime
    public String getLoadbalancerUrl(String loadbalancerServiceName, String fallbackServiceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(loadbalancerServiceName);
        if(!instances.isEmpty()){
            ServiceInstance serviceInstance = instances.get(0);
            _logger.debug("Load balancer URL: " + serviceInstance.getUri().toString());
            return serviceInstance.getUri().toString();
        }
        else{
            _logger.warn("There is no load balancer instance with name '" + loadbalancerServiceName + "'. Trying to fall back to '" + fallbackServiceName + "'...");
            List<ServiceInstance> fallbackInstances = discoveryClient.getInstances(fallbackServiceName);
            if(!fallbackInstances.isEmpty()){
                ServiceInstance serviceInstance = fallbackInstances.get(0);
                _logger.debug("Fallback URL: " + serviceInstance.getUri().toString());
                return serviceInstance.getUri().toString();
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
}