package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.schema.configuration.GenericFieldValue;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
public class ServiceController {
    private static final Log _logger = LogFactory.getLog(ServiceController.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private LoadbalancerUtils loadbalancerUtils;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/services/{serviceId}", method = RequestMethod.GET)
    public ResponseEntity<GenericFieldValue> getServiceUrl(Authentication authentication, HttpSession session, @PathVariable String serviceId){
        try{
            GenericFieldValue serviceFieldValue = new GenericFieldValue();

            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(serviceId)){
                    String loadbalancerUrl = loadbalancerUtils.getLoadbalancerUrl(serviceId, serviceId.replace("loadbalancer", "service"), session, false);

                    String message = "Service ID '" + serviceId + "' successfully retrieved.";
                    _logger.debug(message);

                    serviceFieldValue.setMessage(message);
                    serviceFieldValue.setValue(loadbalancerUrl);

                    return new ResponseEntity<>(serviceFieldValue, HttpStatus.OK);
                }
                else{
                    String errorMessage = "Service ID is blank!";
                    _logger.error(errorMessage);

                    serviceFieldValue.setMessage(errorMessage);

                    return new ResponseEntity<>(serviceFieldValue, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                serviceFieldValue.setMessage(errorMessage);

                return new ResponseEntity<>(serviceFieldValue, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while getting the service url. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericFieldValue serviceFieldValue = new GenericFieldValue();
            serviceFieldValue.setMessage(errorMessage);

            return new ResponseEntity<>(serviceFieldValue, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}