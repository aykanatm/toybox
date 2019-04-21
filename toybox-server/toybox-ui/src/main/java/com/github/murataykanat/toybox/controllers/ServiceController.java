package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.configuration.GenericFieldValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ServiceController {
    private static final Log _logger = LogFactory.getLog(ServiceController.class);

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private DiscoveryClient discoveryClient;

    @RequestMapping(value = "/services/{serviceId}", method = RequestMethod.GET)
    public ResponseEntity<GenericFieldValue> getServiceUrl(Authentication authentication, @PathVariable String serviceId){
        _logger.debug("getServiceUrl() >>");
        try{
            GenericFieldValue serviceFieldValue = new GenericFieldValue();

            if(StringUtils.isNotBlank(serviceId)){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                        if(!instances.isEmpty()){
                            if(instances.size() == 1){
                                ServiceInstance serviceInstance = instances.get(0);

                                String message = "Service ID '" + serviceId + "' successfully retrieved.";
                                _logger.debug(message);

                                serviceFieldValue.setMessage(message);
                                serviceFieldValue.setValue(serviceInstance.getUri().toString());

                                _logger.debug("<< getServiceUrl()");
                                return new ResponseEntity<>(serviceFieldValue, HttpStatus.OK);
                            }
                            else{
                                String errorMessage = "Service ID '" + serviceId + "' has more than one instance.";
                                _logger.error(errorMessage);

                                serviceFieldValue.setMessage(errorMessage);

                                _logger.debug("<< getServiceUrl()");
                                return new ResponseEntity<>(serviceFieldValue, HttpStatus.FORBIDDEN);
                            }
                        }
                        else{
                            String errorMessage = "Service ID '" + serviceId + "' is not found.";
                            _logger.error(errorMessage);

                            serviceFieldValue.setMessage(errorMessage);

                            _logger.debug("<< getServiceUrl()");
                            return new ResponseEntity<>(serviceFieldValue, HttpStatus.NOT_FOUND);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        serviceFieldValue.setMessage(errorMessage);

                        _logger.debug("<< getServiceUrl()");
                        return new ResponseEntity<>(serviceFieldValue, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    serviceFieldValue.setMessage(errorMessage);

                    _logger.debug("<< getServiceUrl()");
                    return new ResponseEntity<>(serviceFieldValue, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Service ID is blank!";
                _logger.error(errorMessage);

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
