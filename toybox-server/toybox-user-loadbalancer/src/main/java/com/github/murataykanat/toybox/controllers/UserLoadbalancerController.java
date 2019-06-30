package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.user.UserResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@RibbonClient(name = "toybox-user-loadbalancer")
@RestController
public class UserLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(UserLoadbalancerController.class);

    private static final String userServiceName = "toybox-user-service";

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

    @HystrixCommand(fallbackMethod = "getCurrentUserErrorFallback")
    @RequestMapping(value = "/users/me", method = RequestMethod.GET)
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication, HttpSession session){
        _logger.debug("getCurrentUser()");
        UserResponse userResponse = new UserResponse();
        try{
            if(isSessionValid(authentication)){
                HttpHeaders headers = getHeaders(session);
                String prefix = getPrefix();

                if(StringUtils.isNotBlank(prefix)){
                    _logger.debug("<< getCurrentUser()");
                    return restTemplate.exchange(prefix + userServiceName + "/users/me", HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);
                }
                else{
                    String errorMessage = "Service ID prefix is null!";

                    _logger.error(errorMessage);

                    userResponse.setMessage(errorMessage);

                    _logger.debug("<< getCurrentUser()");
                    return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                userResponse.setMessage(errorMessage);

                _logger.debug("<< getCurrentUser()");
                return new ResponseEntity<>(userResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while getting the current user. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            userResponse.setMessage(errorMessage);

            _logger.debug("<< getCurrentUser()");
            return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<UserResponse> getCurrentUserErrorFallback(Authentication authentication, HttpSession session, Throwable e){
        _logger.debug("getCurrentUserErrorFallback() >>");
        UserResponse userResponse = new UserResponse();

        String errorMessage;
        if(e.getLocalizedMessage() != null){
            errorMessage = "Unable to get the current user. " + e.getLocalizedMessage();
        }
        else{
            errorMessage = "Unable to get response from the user service.";
        }

        _logger.error(errorMessage, e);

        userResponse.setMessage(errorMessage);

        _logger.debug("<< getCurrentUserErrorFallback()");
        return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpHeaders getHeaders(HttpSession session) throws Exception {
        _logger.debug("getHeaders() >>");
        HttpHeaders headers = new HttpHeaders();

        _logger.debug("Session ID: " + session.getId());
        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
        if(token != null){
            _logger.debug("CSRF Token: " + token.getToken());
            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
            headers.set("X-XSRF-TOKEN", token.getToken());

            _logger.debug("<< getHeaders()");
            return headers;
        }
        else{
            throw new Exception("CSRF token is null!");
        }
    }

    private String getPrefix() throws Exception {
        _logger.debug("getPrefix() >>");
        List<ServiceInstance> instances = discoveryClient.getInstances(userServiceName);
        if(!instances.isEmpty()){
            List<Boolean> serviceSecurity = new ArrayList<>();
            for(ServiceInstance serviceInstance: instances){
                serviceSecurity.add(serviceInstance.isSecure());
            }

            boolean result = serviceSecurity.get(0);

            for(boolean isServiceSecure : serviceSecurity){
                result ^= isServiceSecure;
            }

            if(!result){
                String prefix = result ? "https://" : "http://";

                _logger.debug("<< getPrefix() [" + prefix + "]");
                return prefix;
            }
            else{
                String errorMessage = "Not all container services have the same transfer protocol!";
                _logger.error(errorMessage);

                throw new Exception(errorMessage);

            }
        }
        else{
            String errorMessage = "No container services are running!";
            _logger.error(errorMessage);

            throw new Exception(errorMessage);
        }
    }

    private boolean isSessionValid(Authentication authentication){
        String errorMessage;
        List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
                return true;
            }
            else{
                errorMessage = "Username '" + authentication.getName() + "' is not unique!";
            }
        }
        else{
            errorMessage = "No users with username '" + authentication.getName() + " is found!";
        }

        _logger.error(errorMessage);
        return false;
    }
}
