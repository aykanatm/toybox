package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@RibbonClient(name = "toybox-rendition-loadbalancer")
@RestController
public class RenditionLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(RenditionLoadbalancerController.class);

    private static final String renditionServiceName = "toybox-rendition-service";

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

    @HystrixCommand(fallbackMethod = "getUserAvatarErrorFallback")
    @RequestMapping(value = "/renditions/users/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getUserAvatar(HttpSession session, Authentication authentication, @PathVariable String username) {
        _logger.debug("getUserAvatar() >>");
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(username)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, renditionServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< getUserAvatar()");
                        return restTemplate.exchange(prefix + renditionServiceName + "/renditions/users/" + username, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
                    }
                    else{
                        _logger.debug("<< getUserAvatar()");
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Username parameter is blank!";
                    _logger.error(errorMessage);

                    _logger.debug("<< getUserAvatar()");
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                _logger.debug("<< getUserAvatar()");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the rendition of the current user. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< getUserAvatar()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Resource> getUserAvatarErrorFallback(HttpSession session, Authentication authentication, String username, Throwable e){
        _logger.debug("getUserAvatarErrorFallback() >>");
        if(StringUtils.isNotBlank(username)){
            if(e.getLocalizedMessage() != null){
                _logger.error("Unable to retrieve rendition for the current user. " + e.getLocalizedMessage(), e);
            }
            else{
                _logger.error("Unable to get response from the rendition service.", e);
            }

            _logger.debug("<< getUserAvatarErrorFallback()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            _logger.debug("<< getUserAvatarErrorFallback()");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "assetRenditionErrorFallback")
    @RequestMapping(value = "/renditions/assets/{assetId}/{renditionType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getAssetRendition(HttpSession session, Authentication authentication, @PathVariable String assetId, @PathVariable String renditionType){
        _logger.debug("getAssetRendition() >>");
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(StringUtils.isNotBlank(renditionType)){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, renditionServiceName);

                        if(StringUtils.isNotBlank(prefix)){
                            _logger.debug("<< getAssetRendition()");
                            return restTemplate.exchange(prefix + renditionServiceName + "/renditions/assets/" + assetId + "/" + renditionType, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
                        }
                        else{
                            _logger.debug("<< getAssetRendition()");
                            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                    else{
                        String errorMessage = "Rendition type is blank!";
                        _logger.error(errorMessage);

                        _logger.debug("<< getAssetRendition()");
                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Asset ID is blank!";
                    _logger.error(errorMessage);

                    _logger.debug("<< getAssetRendition()");
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                _logger.debug("<< getAssetRendition()");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the rendition of asset with id '" + assetId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< getAssetRendition()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Resource> assetRenditionErrorFallback(HttpSession session, Authentication authentication, String assetId, String renditionType, Throwable e){
        _logger.debug("assetRenditionErrorFallback() >>");

        if(StringUtils.isNotBlank(assetId)){
            if(StringUtils.isNotBlank(renditionType)){
                if(e.getLocalizedMessage() != null){
                    _logger.error("Unable to retrieve " +  renditionType + " rendition for asset with ID '" + assetId + "'. " + e.getLocalizedMessage(), e);
                }
                else{
                    _logger.error("Unable to get response from the rendition service.", e);
                }

                _logger.debug("<< assetRenditionErrorFallback()");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Rendition type is blank!";
                _logger.error(errorMessage);

                _logger.debug("<< assetRenditionErrorFallback()");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Asset ID is blank!";
            _logger.error(errorMessage);

            _logger.debug("<< assetRenditionErrorFallback()");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}