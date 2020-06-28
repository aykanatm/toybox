package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.ribbon.RibbonRetryHttpRequestFactory;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

@RibbonClient(name = "toybox-rendition-loadbalancer")
@RestController
public class RenditionLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(RenditionLoadbalancerController.class);

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(SpringClientFactory springClientFactory, LoadBalancerClient loadBalancerClient){
        this.restTemplate = new RestTemplate();
        RibbonRetryHttpRequestFactory lFactory = new RibbonRetryHttpRequestFactory(springClientFactory, loadBalancerClient);
        restTemplate.setRequestFactory(lFactory);
        return restTemplate;
    }

    private final LoadbalancerUtils loadbalancerUtils;
    private final AuthenticationUtils authenticationUtils;
    private RestTemplate restTemplate;

    public RenditionLoadbalancerController(LoadbalancerUtils loadbalancerUtils, AuthenticationUtils authenticationUtils, RestTemplate restTemplate){
        this.loadbalancerUtils = loadbalancerUtils;
        this.authenticationUtils = authenticationUtils;
        this.restTemplate = restTemplate;
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "getUserAvatarErrorFallback")
    @GetMapping(value = "/renditions/users/{username}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getUserAvatar(HttpSession session, Authentication authentication, @PathVariable String username) {
        try{
            User user = authenticationUtils.getUser(authentication);
            if(user == null){
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            if(StringUtils.isBlank(username)){
                String errorMessage = "Username parameter is blank!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            HttpHeaders headers = authenticationUtils.getHeaders(session);
            String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.RENDITION_SERVICE_NAME);

            if(StringUtils.isBlank(prefix)){
                throw new IllegalArgumentException("Service ID prefix is null!");
            }

            return restTemplate.exchange(prefix + ToyboxConstants.RENDITION_SERVICE_NAME + "/renditions/users/" + username, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            _logger.error(responseJson.get("message").getAsString());
            return new ResponseEntity<>(httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the rendition of the current user. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<Resource> getUserAvatarErrorFallback(HttpSession session, Authentication authentication, String username, Throwable e){
        if(StringUtils.isBlank(username)){
            String errorMessage = "Username parameter is blank!";
            _logger.error(errorMessage);

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(e.getLocalizedMessage() != null){
            _logger.error("Unable to retrieve rendition for the current user. " + e.getLocalizedMessage(), e);
        }
        else{
            _logger.error("Unable to get response from the rendition service.", e);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "assetRenditionErrorFallback")
    @RequestMapping(value = "/renditions/assets/{assetId}/{renditionType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getAssetRendition(HttpSession session, Authentication authentication, @PathVariable String assetId, @PathVariable String renditionType){
        try{
            User user = authenticationUtils.getUser(authentication);
            if(user == null){
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            if(StringUtils.isBlank(assetId) || StringUtils.isBlank(renditionType)){
                String errorMessage = "Asset ID and/or rendition type is are invalid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            HttpHeaders headers = authenticationUtils.getHeaders(session);
            String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.RENDITION_SERVICE_NAME);

            if(StringUtils.isBlank(prefix)){
                throw new IllegalArgumentException("Service ID prefix is null!");
            }

            return restTemplate.exchange(prefix + ToyboxConstants.RENDITION_SERVICE_NAME + "/renditions/assets/" + assetId + "/" + renditionType, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);

        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            _logger.error(responseJson.get("message").getAsString());
            return new ResponseEntity<>(httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the rendition of asset with id '" + assetId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<Resource> assetRenditionErrorFallback(HttpSession session, Authentication authentication, String assetId, String renditionType, Throwable e){
        if(StringUtils.isBlank(assetId) || StringUtils.isBlank(renditionType)){
            String errorMessage = "Asset ID and/or rendition type is are invalid!";
            _logger.error(errorMessage);

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if(e.getLocalizedMessage() != null){
            _logger.error("Unable to retrieve " +  renditionType + " rendition for asset with ID '" + assetId + "'. " + e.getLocalizedMessage(), e);
        }
        else{
            _logger.error("Unable to get response from the rendition service.", e);
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}