package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.schema.user.RetrieveUsersResponse;
import com.github.murataykanat.toybox.schema.user.UserResponse;
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
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-user-loadbalancer")
@RestController
public class UserLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(UserLoadbalancerController.class);

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
    @HystrixCommand(fallbackMethod = "getCurrentUserErrorFallback")
    @RequestMapping(value = "/users/me", method = RequestMethod.GET)
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication, HttpSession session){
        UserResponse userResponse = new UserResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                HttpHeaders headers = authenticationUtils.getHeaders(session);
                String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.USER_SERVICE_NAME);

                if(StringUtils.isNotBlank(prefix)){
                    return restTemplate.exchange(prefix + ToyboxConstants.USER_SERVICE_NAME + "/users/me", HttpMethod.GET, new HttpEntity<>(headers), UserResponse.class);
                }
                else{
                    throw new IllegalArgumentException("Service ID prefix is null!");
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                userResponse.setMessage(errorMessage);

                return new ResponseEntity<>(userResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            userResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(userResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while getting the current user. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            userResponse.setMessage(errorMessage);

            return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<UserResponse> getCurrentUserErrorFallback(Authentication authentication, HttpSession session, Throwable e){
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

        return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "retrieveUsersErrorFallback")
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ResponseEntity<RetrieveUsersResponse> retrieveUsers(Authentication authentication, HttpSession session){
        RetrieveUsersResponse retrieveUsersResponse = new RetrieveUsersResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                HttpHeaders headers = authenticationUtils.getHeaders(session);
                String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.USER_SERVICE_NAME);

                if(StringUtils.isNotBlank(prefix)){
                    return restTemplate.exchange(prefix + ToyboxConstants.USER_SERVICE_NAME + "/users", HttpMethod.GET, new HttpEntity<>(headers), RetrieveUsersResponse.class);
                }
                else{
                    throw new IllegalArgumentException("Service ID prefix is null!");
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveUsersResponse.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveUsersResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            retrieveUsersResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(retrieveUsersResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving users. " + e.getLocalizedMessage();
            _logger.debug(errorMessage, e);

            retrieveUsersResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveUsersResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveUsersResponse> retrieveUsersErrorFallback(Authentication authentication, HttpSession session, Throwable e){
        RetrieveUsersResponse retrieveUsersResponse = new RetrieveUsersResponse();

        String errorMessage;
        if(e.getLocalizedMessage() != null){
            errorMessage = "Unable to retrieve users. " + e.getLocalizedMessage();
        }
        else{
            errorMessage = "Unable to get response from the user service.";
        }

        _logger.error(errorMessage, e);

        retrieveUsersResponse.setMessage(errorMessage);

        return new ResponseEntity<>(retrieveUsersResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}