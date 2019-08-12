package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.notification.SearchNotificationsRequest;
import com.github.murataykanat.toybox.schema.notification.SearchNotificationsResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.notification.UpdateNotificationsRequest;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-notification-loadbalancer")
@RestController
public class NotificationLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(NotificationLoadbalancerController.class);

    private static final String notificationServiceName = "toybox-notification-service";

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

    @HystrixCommand(fallbackMethod = "sendNotificationErrorFallback")
    @RequestMapping(value = "/notifications", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> sendNotification(Authentication authentication, HttpSession session, @RequestBody SendNotificationRequest sendNotificationRequest){
        _logger.debug("sendNotification() >>");
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(sendNotificationRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, notificationServiceName);

                    _logger.debug("<< sendNotification()");
                    return restTemplate.exchange(prefix + notificationServiceName + "/notifications", HttpMethod.POST, new HttpEntity<>(sendNotificationRequest, headers), GenericResponse.class);
                }
                else{
                    String errorMessage = "Send notification request parameter is null.";
                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< sendNotification()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< sendNotification()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while sending notification. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< sendNotification()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> sendNotificationErrorFallback(Authentication authentication, HttpSession session, SendNotificationRequest sendNotificationRequest, Throwable e){
        _logger.debug("sendNotificationErrorFallback() >>");
        GenericResponse genericResponse = new GenericResponse();
        if(sendNotificationRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to send notification. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the notification service.";
            }

            _logger.error(errorMessage, e);
            genericResponse.setMessage(errorMessage);
            _logger.debug("<< sendNotificationErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Send notification request parameter is null.";
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< sendNotificationErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "searchNotificationsErrorFallback")
    @RequestMapping(value = "/notifications/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchNotificationsResponse> searchNotifications(Authentication authentication, HttpSession session, @RequestBody SearchNotificationsRequest searchNotificationsRequest){
        _logger.debug("searchNotifications() >>");
        SearchNotificationsResponse searchNotificationsResponse = new SearchNotificationsResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(searchNotificationsRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, notificationServiceName);

                    _logger.debug("<< searchNotifications()");
                    return restTemplate.exchange(prefix + notificationServiceName + "/notifications/search", HttpMethod.POST, new HttpEntity<>(searchNotificationsRequest, headers), SearchNotificationsResponse.class);
                }
                else{
                    String errorMessage = "Search notifications request parameter is null.";
                    searchNotificationsResponse.setMessage(errorMessage);

                    _logger.debug("<< searchNotifications()");
                    return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                searchNotificationsResponse.setMessage(errorMessage);

                _logger.debug("<< searchNotifications()");
                return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while searching notifications. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            searchNotificationsResponse.setMessage(errorMessage);

            _logger.debug("<< searchNotifications()");
            return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<SearchNotificationsResponse> searchNotificationsErrorFallback(Authentication authentication, HttpSession session, SearchNotificationsRequest searchNotificationsRequest, Throwable e){
        _logger.debug("searchNotificationsErrorFallback() >>");
        SearchNotificationsResponse searchNotificationsResponse = new SearchNotificationsResponse();

        if(searchNotificationsRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to search notifications. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the notification service.";
            }

            _logger.error(errorMessage, e);
            searchNotificationsResponse.setMessage(errorMessage);
            _logger.debug("<< searchNotificationsErrorFallback()");
            return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Search notifications request parameter is null.";
            searchNotificationsResponse.setMessage(errorMessage);

            _logger.debug("<< searchNotificationsErrorFallback()");
            return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "updateNotificationsErrorFallback")
    @RequestMapping(value = "/notifications", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateNotifications(Authentication authentication, HttpSession session, @RequestBody UpdateNotificationsRequest updateNotificationsRequest){
        _logger.debug("updateNotifications() >>");
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(updateNotificationsRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, notificationServiceName);

                    _logger.debug("<< updateNotifications()");
                    return restTemplate.exchange(prefix + notificationServiceName + "/notifications", HttpMethod.PATCH, new HttpEntity<>(updateNotificationsRequest, headers), GenericResponse.class);
                }
                else{
                    String errorMessage = "Update notifications request parameter is null.";
                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< updateNotifications()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< updateNotifications()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while updating notifications. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< updateNotifications()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> updateNotificationsErrorFallback(Authentication authentication, HttpSession session, UpdateNotificationsRequest updateNotificationsRequest, Throwable e){
        _logger.debug("updateNotificationsErrorFallback() >>");
        GenericResponse genericResponse = new GenericResponse();
        if(updateNotificationsRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to update notifications. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the notification service.";
            }

            _logger.error(errorMessage, e);
            genericResponse.setMessage(errorMessage);
            _logger.debug("<< updateNotificationsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Update notifications request parameter is null.";
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< updateNotificationsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }
}