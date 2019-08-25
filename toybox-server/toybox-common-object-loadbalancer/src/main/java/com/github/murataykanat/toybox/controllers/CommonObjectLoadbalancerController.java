package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.CopyAssetRequest;
import com.github.murataykanat.toybox.schema.asset.MoveAssetRequest;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.github.murataykanat.toybox.utilities.SelectionUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import org.springframework.web.client.HttpStatusCodeException;
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
                        throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            _logger.error(responseJson.get("message").getAsString());
            return new ResponseEntity<>(httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading objects. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<Resource> downloadObjectsErrorFallback(Authentication authentication, HttpSession session, SelectionContext selectionContext, Throwable e){
        if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable download selected objects. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the common object service.";
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
        GenericResponse genericResponse = new GenericResponse();

        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/delete", HttpMethod.POST, new HttpEntity<>(selectionContext, headers), GenericResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while deleting objects. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> deleteObjectsErrorFallback(Authentication authentication, HttpSession session, SelectionContext selectionContext, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to delete selected objects. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the common object service.";
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selection context is not valid!";

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "subscribeToObjectsErrorFallback")
    @RequestMapping(value = "/common-objects/subscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> subscribeToObjects(HttpSession session, Authentication authentication, @RequestBody SelectionContext selectionContext){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/subscribe", HttpMethod.POST, new HttpEntity<>(selectionContext, headers), GenericResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while subscribing to objects. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> subscribeToObjectsErrorFallback(HttpSession session, Authentication authentication, SelectionContext selectionContext, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to subscribe to the selected objects. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the common object service.";
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selection context is not valid!";

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "unsubscribeFromObjectsErrorFallback")
    @RequestMapping(value = "/common-objects/unsubscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> unsubscribeFromObjects(HttpSession session, Authentication authentication, @RequestBody SelectionContext selectionContext){
        GenericResponse genericResponse = new GenericResponse();

        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/unsubscribe", HttpMethod.POST, new HttpEntity<>(selectionContext, headers), GenericResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while unsubscribing from objects. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> unsubscribeFromObjectsErrorFallback(HttpSession session, Authentication authentication, SelectionContext selectionContext, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to unsubscribe from the selected objects. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the common object service.";
            }

            _logger.error(errorMessage, e);


            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selection context is not valid!";

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "moveItemsErrorFallback")
    @RequestMapping(value = "/common-objects/move", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> moveItems(Authentication authentication, HttpSession session, @RequestBody MoveAssetRequest moveAssetRequest){
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(moveAssetRequest != null){
                    SelectionContext selectionContext = moveAssetRequest.getSelectionContext();
                    if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);
                        if(StringUtils.isNotBlank(prefix)){
                            return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/move", HttpMethod.POST, new HttpEntity<>(moveAssetRequest, headers), GenericResponse.class);
                        }
                        else{
                            throw new IllegalArgumentException("Service ID prefix is null!");
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
                    String errorMessage = "Asset move request is null!";
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while moving objects. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> moveItemsErrorFallback(Authentication authentication, HttpSession session, MoveAssetRequest moveAssetRequest, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(moveAssetRequest != null){
            SelectionContext selectionContext = moveAssetRequest.getSelectionContext();
            if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                String errorMessage;
                if(e.getLocalizedMessage() != null){
                    errorMessage = "Unable to move the selected objects. " + e.getLocalizedMessage();
                }
                else{
                    errorMessage = "Unable to get response from the common object service.";
                }

                _logger.error(errorMessage, e);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Selection context is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Move asset request is null!";
            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "copyItemsErrorFallback")
    @RequestMapping(value = "/common-objects/copy", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> copyItems(Authentication authentication, HttpSession session, @RequestBody CopyAssetRequest copyAssetRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(copyAssetRequest != null){
                    SelectionContext selectionContext = copyAssetRequest.getSelectionContext();
                    if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, commonObjectServiceName);
                        if(StringUtils.isNotBlank(prefix)){
                            return restTemplate.exchange(prefix + commonObjectServiceName + "/common-objects/copy", HttpMethod.POST, new HttpEntity<>(copyAssetRequest, headers), GenericResponse.class);
                        }
                        else{
                            throw new IllegalArgumentException("Service ID prefix is null!");
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
                    String errorMessage = "Copy move request is null!";
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while copying objects. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> copyItemsErrorFallback(Authentication authentication, HttpSession session, CopyAssetRequest copyAssetRequest, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(copyAssetRequest != null){
            SelectionContext selectionContext = copyAssetRequest.getSelectionContext();
            if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                String errorMessage;
                if(e.getLocalizedMessage() != null){
                    errorMessage = "Unable to copy the selected objects. " + e.getLocalizedMessage();
                }
                else{
                    errorMessage = "Unable to get response from the common objects service.";
                }

                _logger.error(errorMessage, e);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Selection context is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Copy asset request is null!";
            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }
}