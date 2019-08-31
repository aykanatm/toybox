package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
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
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
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
}