package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.InternalShare;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.utilities.AssetUtils;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.NotificationUtils;
import com.github.murataykanat.toybox.utilities.ShareUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@RestController
public class RenditionController {
    private static final Log _logger = LogFactory.getLog(RenditionController.class);

    @Autowired
    private AssetUtils assetUtils;
    @Autowired
    private NotificationUtils notificationUtils;
    @Autowired
    private ShareUtils shareUtils;

    @Autowired
    private AuthenticationUtils authenticationUtils;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/renditions/users/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getUserAvatar(Authentication authentication, @PathVariable String username){
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(username)){
                    if(username.equalsIgnoreCase("me")){
                        username = authentication.getName();
                    }

                    User user = authenticationUtils.getUser(username);

                    InputStreamResource resource;
                    if(StringUtils.isNotBlank(user.getAvatarPath())){
                        File file = new File(user.getAvatarPath());
                        resource = new InputStreamResource(new FileInputStream(file));
                    }
                    else{
                        _logger.error("User avatar path is blank!");

                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }

                    return new ResponseEntity<>(resource, HttpStatus.OK);
                }
                else{
                    String errorMessage = "Username parameter is blank!";
                    _logger.error(errorMessage);

                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the user with username " + username + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/renditions/assets/{assetId}/{renditionType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getRendition(Authentication authentication, HttpSession session, @PathVariable String assetId, @PathVariable String renditionType){
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    if(StringUtils.isNotBlank(assetId) || StringUtils.isNotBlank(renditionType)){
                        Asset asset = assetUtils.getAsset(assetId);

                        if(renditionType.equalsIgnoreCase("t")){
                            if(StringUtils.isNotBlank(asset.getThumbnailPath())){
                                File file = new File(asset.getThumbnailPath());
                                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                                return new ResponseEntity<>(resource, HttpStatus.OK);
                            }
                            else{
                                _logger.error("Thumbnail path is blank!");

                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                            }
                        }
                        else if(renditionType.equalsIgnoreCase("p")){
                            if(StringUtils.isNotBlank(asset.getPreviewPath())){
                                if(asset.getPreviewPath().toLowerCase().endsWith("pdf")){
                                    File file = new File(asset.getPreviewPath());

                                    HttpHeaders httpHeaders = new HttpHeaders();
                                    httpHeaders.add("Content-Disposition", "inline; filename=" + asset.getName());
                                    httpHeaders.add("Content-Type", MediaType.APPLICATION_PDF_VALUE);

                                    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                                    return new ResponseEntity<>(resource, httpHeaders, HttpStatus.OK);
                                }
                                else{
                                    File file = new File(asset.getPreviewPath());
                                    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                                    return new ResponseEntity<>(resource, HttpStatus.OK);
                                }
                            }
                            else{
                                _logger.error("Preview path is blank!");

                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                            }
                        }
                        else if(renditionType.equalsIgnoreCase("o")){
                            if(StringUtils.isNotBlank(asset.getPath())){
                                boolean canDownload = shareUtils.canDownload(user.getId(), asset.getId(), true);

                                if(canDownload){
                                    List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), asset.getId(), true);
                                    for(InternalShare internalShare: internalShares){
                                        boolean downloadAllowed = internalShare.getCanDownload().equalsIgnoreCase("Y");
                                        boolean notifyOnDownload = internalShare.getNotifyOnDownload().equalsIgnoreCase("Y");
                                        if(downloadAllowed && notifyOnDownload){
                                            String message = "Asset '" + asset.getName() + "' is downloaded by '" + user.getUsername() + "'";
                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                            sendNotificationRequest.setFromUsername(user.getUsername());
                                            sendNotificationRequest.setToUsername(internalShare.getUsername());
                                            sendNotificationRequest.setMessage(message);
                                            notificationUtils.sendNotification(sendNotificationRequest, session);
                                        }
                                    }

                                    File file = new File(asset.getPath());
                                    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                                    return new ResponseEntity<>(resource, HttpStatus.OK);
                                }
                                else{
                                    String errorMessage = "You do not have the permission to download this file!";
                                    _logger.error(errorMessage);

                                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                                }
                            }
                            else{
                                _logger.error("Original asset path is blank!");

                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                            }
                        }
                        else{
                            throw new IllegalArgumentException("Rendition type is not recognized!");
                        }
                    }
                    else{
                        String errorMessage = "Parameters are invalid!";
                        _logger.error(errorMessage);

                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    throw new IllegalArgumentException("User is null!");
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the asset with ID " + assetId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}