package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.ribbon.RibbonRetryHttpRequestFactory;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.share.*;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-share-loadbalancer")
@RestController
public class ShareLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(ShareLoadbalancerController.class);

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(SpringClientFactory springClientFactory, LoadBalancerClient loadBalancerClient){
        this.restTemplate = new RestTemplate();
        RibbonRetryHttpRequestFactory lFactory = new RibbonRetryHttpRequestFactory(springClientFactory, loadBalancerClient);
        restTemplate.setRequestFactory(lFactory);
        return restTemplate;
    }

    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;

    @Autowired
    private RestTemplate restTemplate;

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "downloadExternalShareErrorFallback")
    @RequestMapping(value = "/share/download/{externalShareId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadExternalShare(@PathVariable String externalShareId){
        try{
            if(StringUtils.isNotBlank(externalShareId)){
                String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.SHARE_SERVICE_NAME);
                HttpHeaders headers = new HttpHeaders();

                if(StringUtils.isNotBlank(prefix)){
                    return restTemplate.exchange(prefix + ToyboxConstants.SHARE_SERVICE_NAME + "/share/download/" + externalShareId, HttpMethod.GET, new HttpEntity<>(headers),Resource.class);
                }
                else{
                    throw new IllegalArgumentException("Service ID prefix is null!");
                }
            }
            else{
                String errorMessage = "External share ID is blank! ";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            _logger.error(responseJson.get("message").getAsString());
            return new ResponseEntity<>(httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading the shared assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<Resource> downloadExternalShareErrorFallback(String externalShareId, Throwable e){
        if(StringUtils.isNotBlank(externalShareId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to create external share. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the share service.";
            }

            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "External share ID is blank! ";
            _logger.error(errorMessage);

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "createExternalShareErrorFallback")
    @RequestMapping(value = "/share/external", method = RequestMethod.POST)
    public ResponseEntity<ExternalShareResponse> createExternalShare(Authentication authentication, HttpSession session, @RequestBody ExternalShareRequest externalShareRequest) {
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(externalShareRequest != null){
                    HttpHeaders headers = authenticationUtils.getHeaders(session);
                    String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.SHARE_SERVICE_NAME);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + ToyboxConstants.SHARE_SERVICE_NAME + "/share/external", HttpMethod.POST, new HttpEntity<>(externalShareRequest, headers), ExternalShareResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
                    }
                }
                else{
                    String errorMessage = "External share request is null!";
                    _logger.error(errorMessage);

                    externalShareResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(externalShareResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                externalShareResponse.setMessage(errorMessage);

                return new ResponseEntity<>(externalShareResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            externalShareResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(externalShareResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while sharing assets externally. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            externalShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(externalShareResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<ExternalShareResponse> createExternalShareErrorFallback(Authentication authentication, HttpSession session, ExternalShareRequest externalShareRequest, Throwable e){
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();

        if(externalShareRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to create external share. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the share service.";
            }

            _logger.error(errorMessage, e);

            externalShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(externalShareResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "External share request is null!";

            _logger.error(errorMessage);

            externalShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(externalShareResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "createInternalShareErrorFallback")
    @RequestMapping(value = "/share/internal", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> createInternalShare(Authentication authentication, HttpSession session, @RequestBody InternalShareRequest internalShareRequest) {
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(internalShareRequest != null){
                    HttpHeaders headers = authenticationUtils.getHeaders(session);
                    String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.SHARE_SERVICE_NAME);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + ToyboxConstants.SHARE_SERVICE_NAME + "/share/internal", HttpMethod.POST, new HttpEntity<>(internalShareRequest, headers), GenericResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
                    }
                }
                else{
                    String errorMessage = "Internal share request is null!";
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
            String errorMessage = "An error occurred while sharing assets internally. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> createInternalShareErrorFallback(Authentication authentication, HttpSession session, InternalShareRequest internalShareRequest, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(internalShareRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to create internal share. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the share service.";
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Internal share request is null!";

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "searchSharesErrorFallback")
    @RequestMapping(value = "/share/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveSharesResponse> searchShares(Authentication authentication, HttpSession session, @RequestBody ShareSearchRequest shareSearchRequest){
        RetrieveSharesResponse retrieveSharesResponse = new RetrieveSharesResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(shareSearchRequest != null){
                    HttpHeaders headers = authenticationUtils.getHeaders(session);
                    String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.SHARE_SERVICE_NAME);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + ToyboxConstants.SHARE_SERVICE_NAME + "/share/search", HttpMethod.POST, new HttpEntity<>(shareSearchRequest, headers), RetrieveSharesResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
                    }
                }
                else{
                    String errorMessage = "Internal share request is null!";
                    _logger.error(errorMessage);

                    retrieveSharesResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveSharesResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveSharesResponse.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveSharesResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            retrieveSharesResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(retrieveSharesResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while searching shares. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveSharesResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveSharesResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveSharesResponse> searchSharesErrorFallback(Authentication authentication, HttpSession session, ShareSearchRequest shareSearchRequest, Throwable e){
        RetrieveSharesResponse retrieveSharesResponse = new RetrieveSharesResponse();

        if(shareSearchRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to search shares. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the share service.";
            }

            _logger.error(errorMessage, e);

            retrieveSharesResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveSharesResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Share search request is null!";

            _logger.error(errorMessage);

            retrieveSharesResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveSharesResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "getSharesErrorFallback")
    @RequestMapping(value = "/share/{id}", method = RequestMethod.GET)
    public ResponseEntity<RetrieveShareResponse> getShare(Authentication authentication, HttpSession session, @PathVariable String id, @RequestParam("type") String type) {
        RetrieveShareResponse retrieveShareResponse = new RetrieveShareResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(id)  && StringUtils.isNotBlank(type)){
                    HttpHeaders headers = authenticationUtils.getHeaders(session);
                    String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.SHARE_SERVICE_NAME);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + ToyboxConstants.SHARE_SERVICE_NAME + "/share/" + id + "?type=" + type, HttpMethod.GET, new HttpEntity<>(headers), RetrieveShareResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
                    }
                }
                else{
                    String errorMessage = "Internal share request is null!";
                    _logger.error(errorMessage);

                    retrieveShareResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveShareResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveShareResponse.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveShareResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            retrieveShareResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(retrieveShareResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while getting a share. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveShareResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveShareResponse> getSharesErrorFallback(Authentication authentication, HttpSession session, @PathVariable String id, @RequestParam("type") String type, Throwable e){
        RetrieveShareResponse retrieveShareResponse = new RetrieveShareResponse();

        if(StringUtils.isNotBlank(id) && StringUtils.isNotBlank(type)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to retrieve a share. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the share service.";
            }

            _logger.error(errorMessage, e);

            retrieveShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveShareResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Id or Type is blank!";

            _logger.error(errorMessage);

            retrieveShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveShareResponse, HttpStatus.BAD_REQUEST);
        }
    }
}