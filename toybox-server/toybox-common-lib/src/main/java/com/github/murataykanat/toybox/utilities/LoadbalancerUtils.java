package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.List;

public class LoadbalancerUtils {
    private static final Log _logger = LogFactory.getLog(LoadbalancerUtils.class);

    private static LoadbalancerUtils loadbalancerUtils;

    private LoadbalancerUtils(){}

    public static LoadbalancerUtils getInstance(){
        if(loadbalancerUtils != null){
            return loadbalancerUtils;
        }
        else{
            return new LoadbalancerUtils();
        }
    }

    @LogEntryExitExecutionTime
    public String getLoadbalancerUrl(DiscoveryClient discoveryClient, String loadbalancerServiceName) throws Exception {
        List<ServiceInstance> instances = discoveryClient.getInstances(loadbalancerServiceName);
        if(!instances.isEmpty()){
            ServiceInstance serviceInstance = instances.get(0);
            _logger.debug("Load balancer URL: " + serviceInstance.getUri().toString());
            return serviceInstance.getUri().toString();
        }
        else{
            throw new Exception("There is no load balancer instance with name '" + loadbalancerServiceName + "'.");
        }
    }

    @LogEntryExitExecutionTime
    public String getPrefix(DiscoveryClient discoveryClient, String loadbalancerServiceName) throws Exception {
        List<ServiceInstance> instances = discoveryClient.getInstances(loadbalancerServiceName);
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
                String errorMessage = "Not all " + loadbalancerServiceName + " services have the same transfer protocol!";
                _logger.error(errorMessage);

                throw new Exception(errorMessage);

            }
        }
        else{
            String errorMessage = "No " + loadbalancerServiceName + " services are running!";
            _logger.error(errorMessage);

            throw new Exception(errorMessage);
        }
    }
}
