package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.schema.configuration.GenericFieldValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ServiceController {
    private static final Log _logger = LogFactory.getLog(ServiceController.class);

    @Autowired
    private DiscoveryClient discoveryClient;

    @RequestMapping(value = "/services/{serviceId}", method = RequestMethod.GET)
    public ResponseEntity<GenericFieldValue> getServiceUrl(@PathVariable String serviceId){
        _logger.debug("getServiceUrl() >>");
        try{
            if(StringUtils.isNotBlank(serviceId)){
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                if(!instances.isEmpty()){
                    if(instances.size() == 1){
                        ServiceInstance serviceInstance = instances.get(0);

                        String message = "Service ID '" + serviceId + "' successfully retrieved.";
                        _logger.debug(message);

                        GenericFieldValue serviceFieldValue = new GenericFieldValue();
                        serviceFieldValue.setMessage(message);
                        serviceFieldValue.setValue(serviceInstance.getUri().toString());

                        _logger.debug("<< getServiceUrl()");
                        return new ResponseEntity<>(serviceFieldValue, HttpStatus.OK);
                    }
                    else{
                        String errorMessage = "Service ID '" + serviceId + "' has more than one instance.";
                        _logger.error(errorMessage);

                        GenericFieldValue serviceFieldValue = new GenericFieldValue();
                        serviceFieldValue.setMessage(errorMessage);

                        _logger.debug("<< getServiceUrl()");
                        return new ResponseEntity<>(serviceFieldValue, HttpStatus.FORBIDDEN);
                    }
                }
                else{
                    String errorMessage = "Service ID '" + serviceId + "' is not found.";
                    _logger.error(errorMessage);

                    GenericFieldValue serviceFieldValue = new GenericFieldValue();
                    serviceFieldValue.setMessage(errorMessage);

                    _logger.debug("<< getServiceUrl()");
                    return new ResponseEntity<>(serviceFieldValue, HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "Service ID is blank!";
                _logger.error(errorMessage);

                GenericFieldValue serviceFieldValue = new GenericFieldValue();
                serviceFieldValue.setMessage(errorMessage);

                _logger.debug("<< getServiceUrl()");
                return new ResponseEntity<>(serviceFieldValue, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while getting the service url. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericFieldValue serviceFieldValue = new GenericFieldValue();
            serviceFieldValue.setMessage(errorMessage);

            _logger.debug("<< getServiceUrl()");
            return new ResponseEntity<>(serviceFieldValue, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
