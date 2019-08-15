package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.github.murataykanat.toybox.utilities.SelectionUtils;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-common-object-loadbalancer")
@RestController
public class CommonObjectLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(CommonObjectLoadbalancerController.class);

    private static final String commonObjectServiceName = "toybox-common-object-service";

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
    }

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RestTemplate restTemplate;

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "downloadObjectsErrorFallback")
    @RequestMapping(value = "/common-objects/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadObjects(Authentication authentication, HttpSession session, @RequestBody SelectionContext selectionContext){
        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/download", HttpMethod.POST, new HttpEntity<>(selectionContext, headers), Resource.class);
                    }
                    else{
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selection context is not valid!";
                    _logger.error(errorMessage);

                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading the assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<Resource> downloadObjectsErrorFallback(Authentication authentication, HttpSession session, SelectionContext selectionContext, Throwable e){
        if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable download selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selection context is not valid!";
            _logger.error(errorMessage);

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "deleteObjectsErrorFallback")
    @RequestMapping(value = "/common-objects/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> deleteObjects(Authentication authentication, HttpSession session, @RequestBody SelectionContext selectionContext){
        try {
            GenericResponse genericResponse = new GenericResponse();
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/delete", HttpMethod.POST, new HttpEntity<>(selectionContext, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selection context is not valid!";

                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while deleting assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> deleteObjectsErrorFallback(Authentication authentication, HttpSession session, SelectionContext selectionContext, Throwable e){
        if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to delete selected objects. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the common object service.";
            }

            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selection context is not valid!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "subscribeToObjectsErrorFallback")
    @RequestMapping(value = "/common-objects/subscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> subscribeToObjects(HttpSession session, Authentication authentication, @RequestBody SelectionContext selectionContext){
        try{
            GenericResponse genericResponse = new GenericResponse();

            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/subscribe", HttpMethod.POST, new HttpEntity<>(selectionContext, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selection context is not valid!";

                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while subscribing to assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> subscribeToObjectsErrorFallback(HttpSession session, Authentication authentication, SelectionContext selectionContext, Throwable e){
        if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to subscribe to the selected objects. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the common object service.";
            }

            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selection context is not valid!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }
}