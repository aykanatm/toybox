package com.github.murataykanat.toybox.controllers;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-rendition-loadbalancer")
@RestController
public class RenditionLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(RenditionLoadbalancerController.class);

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
    }

    @Autowired
    private RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "userRenditionErrorFallback")
    @RequestMapping(value = "/renditions/users/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getLoadBalancedUserAvatar(HttpSession session, @PathVariable String username){
        _logger.debug("getLoadBalancedUserAvatar() >>");

        _logger.debug("Session ID: " + session.getId());
        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
        _logger.debug("CSRF Token: " + token.getToken());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
        headers.set("X-XSRF-TOKEN", token.getToken());

        _logger.debug("<< getLoadBalancedUserAvatar()");
        return restTemplate.exchange("http://toybox-rendition-service/renditions/users/" + username, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
    }

    @HystrixCommand(fallbackMethod = "assetRenditionErrorFallback")
    @RequestMapping(value = "/renditions/assets/{assetId}/{renditionType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getLoadBalancedRendition(HttpSession session, @PathVariable String assetId, @PathVariable String renditionType){
        _logger.debug("getLoadBalancedRendition() >>");

        _logger.debug("Session ID: " + session.getId());
        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
        _logger.debug("CSRF Token: " + token.getToken());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
        headers.set("X-XSRF-TOKEN", token.getToken());

        _logger.debug("<< getLoadBalancedRendition()");
        return restTemplate.exchange("http://toybox-rendition-service/renditions/assets/" + assetId + "/" + renditionType, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
    }

    public ResponseEntity<Resource> userRenditionErrorFallback(HttpSession session, @PathVariable String username){
        _logger.debug("userRenditionErrorFallback() >>");

        _logger.error("Unable to retrieve rendition for username '" + username + "'. Please check if the any of the rendition services are running.");

        _logger.debug("<< userRenditionErrorFallback()");
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<Resource> assetRenditionErrorFallback(HttpSession session, @PathVariable String assetId, @PathVariable String renditionType){
        _logger.debug("assetRenditionErrorFallback() >>");

        _logger.error("Unable to retrieve " +  renditionType + " rendition for asset with ID '" + assetId + "'. Please check if the any of the rendition services are running.");

        _logger.debug("<< assetRenditionErrorFallback()");
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
