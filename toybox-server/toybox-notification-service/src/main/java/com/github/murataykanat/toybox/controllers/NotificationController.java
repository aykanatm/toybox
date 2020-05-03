package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.models.search.FacetField;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.dbo.Notification;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.notification.NotificationSearchRequest;
import com.github.murataykanat.toybox.schema.notification.NotificationSearchResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.notification.NotificationUpdateRequest;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import com.github.murataykanat.toybox.utilities.NotificationUtils;
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
import java.util.Date;
import java.util.List;

@RestController
public class NotificationController {
    private static final Log _logger = LogFactory.getLog(NotificationController.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private FacetUtils facetUtils;
    @Autowired
    private NotificationUtils notificationUtils;

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
                        notification.setIsRead(ToyboxConstants.LOOKUP_NO);
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
    public ResponseEntity<NotificationSearchResponse> searchNotifications(Authentication authentication, @RequestBody NotificationSearchRequest notificationSearchRequest){
        NotificationSearchResponse notificationSearchResponse = new NotificationSearchResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(notificationSearchRequest != null){
                    String sortField = notificationSearchRequest.getSortColumn();
                    String sortType = notificationSearchRequest.getSortType();
                    int offset = notificationSearchRequest.getOffset();
                    int limit = notificationSearchRequest.getLimit();

                    List<SearchCondition> searchConditions = notificationSearchRequest.getSearchConditions();
                    if(searchConditions == null){
                        searchConditions = new ArrayList<>();
                    }

                    List<SearchRequestFacet> searchRequestFacetList = notificationSearchRequest.getSearchRequestFacetList();

                    if(searchRequestFacetList != null){
                        for (SearchRequestFacet searchRequestFacet: searchRequestFacetList){
                            String fieldName = searchRequestFacet.getFieldName();
                            FacetField facetField = facetUtils.getFacetField(fieldName, new Notification());

                            String dbFieldName = facetField.getFieldName();
                            String fieldValue = searchRequestFacet.getFieldValue();
                            searchConditions.add(new SearchCondition(dbFieldName, ToyboxConstants.SEARCH_CONDITION_EQUALS, fieldValue,
                                    facetField.getDataType(), ToyboxConstants.SEARCH_OPERATOR_AND));
                        }
                    }

                    searchConditions.add(new SearchCondition("username", ToyboxConstants.SEARCH_CONDITION_EQUALS, authentication.getName(),
                            ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_AND));


                    List<Notification> notifications = notificationUtils.getNotifications(searchConditions, sortField, sortType);

                    if(!notifications.isEmpty()){
                        List<Facet> facets = facetUtils.getFacets(notifications);

                        notificationSearchResponse.setFacets(facets);

                        int totalRecords = notifications.size();
                        int endIndex = Math.min((offset + limit), totalRecords);

                        List<Notification> notificationsOnPage = notifications.subList(offset, endIndex);

                        notificationSearchResponse.setTotalRecords(totalRecords);
                        notificationSearchResponse.setNotifications(notificationsOnPage);
                        notificationSearchResponse.setMessage("Notifications were retrieved successfully!");

                        return new ResponseEntity<>(notificationSearchResponse, HttpStatus.OK);
                    }
                    else{
                        String message = "There is no notifications to return.";
                        _logger.debug(message);

                        notificationSearchResponse.setMessage(message);

                        return new ResponseEntity<>(notificationSearchResponse, HttpStatus.NO_CONTENT);
                    }
                }
                else{
                    String errorMessage = "Search notifications request is null!";
                    _logger.error(errorMessage);

                    notificationSearchResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(notificationSearchResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                notificationSearchResponse.setMessage(errorMessage);

                return new ResponseEntity<>(notificationSearchResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while searching for notifications. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            notificationSearchResponse.setMessage(errorMessage);

            return new ResponseEntity<>(notificationSearchResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/notifications", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateNotifications(Authentication authentication, @RequestBody NotificationUpdateRequest notificationUpdateRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(notificationUpdateRequest != null){
                    if(!notificationUpdateRequest.getNotificationIds().isEmpty()){
                        if(notificationUpdateRequest.getNotificationIds().get(0) == 0){
                            notificationsRepository.updateAllNotifications(notificationUpdateRequest.getIsRead());
                        }
                        else{
                            notificationsRepository.updateNotifications(notificationUpdateRequest.getIsRead(), notificationUpdateRequest.getNotificationIds());
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