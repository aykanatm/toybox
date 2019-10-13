package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.dbo.Notification;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.notification.SearchNotificationsRequest;
import com.github.murataykanat.toybox.schema.notification.SearchNotificationsResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.notification.UpdateNotificationsRequest;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import com.github.murataykanat.toybox.utilities.SortUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class NotificationController {
    private static final Log _logger = LogFactory.getLog(NotificationController.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private FacetUtils facetUtils;
    @Autowired
    private SortUtils sortUtils;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationsRepository notificationsRepository;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/notifications", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> sendNotification(Authentication authentication, @RequestBody SendNotificationRequest sendNotificationRequest){
        GenericResponse genericResponse = new GenericResponse();
        int notificationCount = 0;

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(sendNotificationRequest != null){
                    String toUsername = sendNotificationRequest.getToUsername();
                    String fromUsername = sendNotificationRequest.getFromUsername();

                    if(!authentication.getName().equalsIgnoreCase(toUsername)){
                        Notification notification = new Notification();
                        notification.setUsername(toUsername);
                        notification.setNotification(sendNotificationRequest.getMessage());
                        notification.setIsRead("N");
                        notification.setDate(new Date());
                        notification.setFrom(fromUsername);

                        rabbitTemplate.convertAndSend(ToyboxConstants.TOYBOX_NOTIFICATION_EXCHANGE,"toybox.notification." + System.currentTimeMillis(), notification);
                        notificationCount++;
                    }
                    else{
                        _logger.debug("Users cannot send notification to themselves. Skipping...");
                    }

                    if(notificationCount > 0){
                        genericResponse.setMessage(notificationCount + " notification(s) were sent successfully!");
                    }
                    else{
                        genericResponse.setMessage("No notifications were sent.");
                    }

                    return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                }
                else{
                    String errorMessage = "Notification request parameter is null!";
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
        catch (Exception e){
            String errorMessage;
            if(notificationCount > 0){
                errorMessage = notificationCount + " notification(s) were sent successfully but an error occurred while sending notifications. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "An error occurred while sending a notification. " + e.getLocalizedMessage();
            }

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/notifications/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SearchNotificationsResponse> searchNotifications(Authentication authentication, @RequestBody SearchNotificationsRequest searchNotificationsRequest){
        SearchNotificationsResponse searchNotificationsResponse = new SearchNotificationsResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(searchNotificationsRequest != null){
                    int offset = searchNotificationsRequest.getOffset();
                    int limit = searchNotificationsRequest.getLimit();
                    List<SearchRequestFacet> searchRequestFacetList = searchNotificationsRequest.getSearchRequestFacetList();

                    List<Notification> notificationsByUsername = notificationsRepository.getNotificationsByUsername(authentication.getName());

                    if(!notificationsByUsername.isEmpty()){
                        List<Notification> notifications = new ArrayList<>();
                        for(Notification notification: notificationsByUsername){
                            boolean fromUsernameMatch = searchNotificationsRequest.getFrom() == null || (StringUtils.isBlank(searchNotificationsRequest.getFrom()) ? true : notification.getFrom().equalsIgnoreCase(searchNotificationsRequest.getFrom()));
                            boolean contentMatch = searchNotificationsRequest.getContent().equalsIgnoreCase("*") ? true : notification.getNotification().contains(searchNotificationsRequest.getContent());
                            boolean dateMatch = searchNotificationsRequest.getDate() == null ? true : notification.getDate().before(searchNotificationsRequest.getDate());
                            boolean isReadMatch = StringUtils.isBlank(searchNotificationsRequest.getIsRead()) ? true : notification.getIsRead().equalsIgnoreCase(searchNotificationsRequest.getIsRead());

                            if(fromUsernameMatch && contentMatch && dateMatch && isReadMatch){
                                notifications.add(notification);
                            }
                        }

                        List<Notification> facetedNotifications;
                        if(searchRequestFacetList != null && !searchRequestFacetList.isEmpty()){
                            facetedNotifications = notifications.stream().filter(notification -> facetUtils.hasFacetValue(notification, searchRequestFacetList)).collect(Collectors.toList());
                        }
                        else{
                            facetedNotifications = notifications;
                        }

                        List<Facet> facets = facetUtils.getFacets(facetedNotifications);

                        searchNotificationsResponse.setFacets(facets);

                        sortUtils.sortItems("des", facetedNotifications, Comparator.comparing(Notification::getDate, Comparator.nullsLast(Comparator.naturalOrder())));

                        int totalRecords = facetedNotifications.size();
                        int startIndex = offset;
                        int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                        List<Notification> notificationsOnPage = facetedNotifications.subList(startIndex, endIndex);

                        searchNotificationsResponse.setTotalRecords(totalRecords);
                        searchNotificationsResponse.setNotifications(notificationsOnPage);
                        searchNotificationsResponse.setMessage("Notifications were retrieved successfully!");

                        return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.OK);
                    }
                    else{
                        String message = "There is no notifications to return.";
                        _logger.debug(message);

                        searchNotificationsResponse.setMessage(message);

                        return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.NO_CONTENT);
                    }
                }
                else{
                    String errorMessage = "Search notifications request is null!";
                    _logger.error(errorMessage);

                    searchNotificationsResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                searchNotificationsResponse.setMessage(errorMessage);

                return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while searching for notifications. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            searchNotificationsResponse.setMessage(errorMessage);

            return new ResponseEntity<>(searchNotificationsResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/notifications", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateNotifications(Authentication authentication, @RequestBody UpdateNotificationsRequest updateNotificationsRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(updateNotificationsRequest != null){
                    if(!updateNotificationsRequest.getNotificationIds().isEmpty()){
                        if(updateNotificationsRequest.getNotificationIds().get(0) == 0){
                            notificationsRepository.updateAllNotifications(updateNotificationsRequest.getIsRead());
                        }
                        else{
                            notificationsRepository.updateNotifications(updateNotificationsRequest.getIsRead(), updateNotificationsRequest.getNotificationIds());
                        }

                        genericResponse.setMessage("Notifications were updated successfully!");

                        return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                    }
                    else{
                        String message = "There is no notifications to update.";
                        _logger.debug(message);

                        genericResponse.setMessage(message);

                        return new ResponseEntity<>(genericResponse, HttpStatus.NO_CONTENT);
                    }
                }
                else{
                    String errorMessage = "Update notifications request is null.";
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
        catch (Exception e){
            String errorMessage = "An error occurred while updating notifications. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}