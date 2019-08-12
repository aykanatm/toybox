package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
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
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@RibbonClient(name = "toybox-share-loadbalancer")
@RestController
public class ShareLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(ShareLoadbalancerController.class);

    private static final String shareServiceName = "toybox-share-service";

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
    @HystrixCommand(fallbackMethod = "createExternalShareErrorFallback")
    @RequestMapping(value = "/share/external", method = RequestMethod.POST)
    public ResponseEntity<ExternalShareResponse> createExternalShare(Authentication authentication, HttpSession session, @RequestBody ExternalShareRequest externalShareRequest) {
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(externalShareRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, shareServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + shareServiceName + "/share/external", HttpMethod.POST, new HttpEntity<>(externalShareRequest, headers), ExternalShareResponse.class);
                    }
                    else{
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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
