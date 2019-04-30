package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.AssetUser;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.notification.Notification;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
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

import java.util.Date;
import java.util.List;

@RestController
public class NotificationController {
    private static final Log _logger = LogFactory.getLog(NotificationController.class);

    private static final String topicExchangeName = "toybox-notification-exchange";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AssetUserRepository assetUserRepository;
    @Autowired
    private UsersRepository usersRepository;

    @RequestMapping(value = "/notifications", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> sendNotification(Authentication authentication, @RequestBody SendNotificationRequest sendNotificationRequest){
        _logger.debug("sendNotification() >>");
        GenericResponse genericResponse = new GenericResponse();
        int notificationCount = 0;
        try{
            if(sendNotificationRequest != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        List<AssetUser> assetUsersByAssetId = assetUserRepository.findAssetUsersByAssetId(sendNotificationRequest.getAsset().getId());
                        if(assetUsersByAssetId != null && !assetUsersByAssetId.isEmpty()){
                            for(AssetUser assetUser: assetUsersByAssetId){
                                List<User> toUsersByUserId = usersRepository.findUsersByUserId(assetUser.getUserId());
                                if(!toUsersByUserId.isEmpty()){
                                    if(toUsersByUserId.size() == 1){
                                        User toUser = toUsersByUserId.get(0);

                                        String toUsername = toUser.getUsername();
                                        String fromUsername = sendNotificationRequest.getFromUser().getUsername();

                                        Notification notification = new Notification();
                                        notification.setUsername(toUsername);
                                        notification.setNotification(sendNotificationRequest.getMessage());
                                        notification.setIsRead(false);
                                        notification.setDate(new Date());
                                        notification.setFrom(fromUsername);

                                        rabbitTemplate.convertAndSend(topicExchangeName,"toybox.notification." + System.currentTimeMillis(), notification);
                                        notificationCount++;
                                    }
                                    else{
                                        throw new Exception("There are more than one user with ID '" + assetUser.getUserId() + "'.");
                                    }
                                }
                                else{
                                    throw new Exception("There is no user with ID '" + assetUser.getUserId() + "'.");
                                }
                            }

                            genericResponse.setMessage(notificationCount + " notification(s) were sent successfully!");

                            _logger.debug("<< sendNotification()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            String errorMessage = "No users associated with asset with ID '" + sendNotificationRequest.getAsset().getId() + "' is found.";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            _logger.debug("<< sendNotification()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< sendNotification()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< sendNotification()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Notification request parameter is null!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< sendNotification()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
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

            _logger.debug("<< sendNotification()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
