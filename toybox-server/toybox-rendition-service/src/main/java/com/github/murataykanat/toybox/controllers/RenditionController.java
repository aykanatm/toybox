package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@RestController
public class RenditionController {
    private static final Log _logger = LogFactory.getLog(RenditionController.class);

    private final AssetUtils assetUtils;
    private final NotificationUtils notificationUtils;
    private final ShareUtils shareUtils;
    private final AuthenticationUtils authenticationUtils;

    public RenditionController(AssetUtils assetUtils, NotificationUtils notificationUtils, ShareUtils shareUtils, AuthenticationUtils authenticationUtils){
        this.assetUtils = assetUtils;
        this.notificationUtils = notificationUtils;
        this.shareUtils = shareUtils;
        this.authenticationUtils = authenticationUtils;
    }

    @LogEntryExitExecutionTime
    @GetMapping(value = "/renditions/users/{username}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getUserAvatar(Authentication authentication, @PathVariable String username){
        try{
            User user = authenticationUtils.getUser(authentication);
            if(user == null){
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            if(StringUtils.isBlank(username)){
                String errorMessage = "Username parameter is blank!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            InputStreamResource resource;
            if(StringUtils.isBlank(user.getAvatarPath())){
                _logger.error("User avatar path is blank!");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            File file = new File(user.getAvatarPath());
            resource = new InputStreamResource(new FileInputStream(file));

            return new ResponseEntity<>(resource, HttpStatus.OK);
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the user with username " + username + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @GetMapping(value = "/renditions/assets/{assetId}/{renditionType}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getRendition(Authentication authentication, HttpSession session, @PathVariable String assetId, @PathVariable String renditionType){
        try{
            User user = authenticationUtils.getUser(authentication);
            if(user == null){
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            if(StringUtils.isBlank(assetId) || StringUtils.isBlank(renditionType)){
                String errorMessage = "Asset ID and/or rendition type is are invalid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            Asset asset = assetUtils.getAsset(assetId);

            if(!renditionType.equalsIgnoreCase("t") && !renditionType.equalsIgnoreCase("p") && !renditionType.equalsIgnoreCase("o")){
                _logger.error("Rendition type " + renditionType + " is invalid!");

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            InputStreamResource resource =  null;
            HttpHeaders httpHeaders = new HttpHeaders();

            if(renditionType.equalsIgnoreCase("t")){
                if(StringUtils.isBlank(asset.getThumbnailPath())){
                    _logger.error("Thumbnail path is blank!");

                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                File file = new File(asset.getThumbnailPath());
                resource = new InputStreamResource(new FileInputStream(file));
            }
            else if(renditionType.equalsIgnoreCase("p")){
                if(StringUtils.isBlank(asset.getPreviewPath())){
                    _logger.error("Preview path is blank!");

                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                File file = new File(asset.getPreviewPath());
                if(asset.getPreviewPath().toLowerCase().endsWith("pdf")){
                    httpHeaders.add("Content-Disposition", "inline; filename=" + asset.getName());
                    httpHeaders.add("Content-Type", MediaType.APPLICATION_PDF_VALUE);
                }

                resource = new InputStreamResource(new FileInputStream(file));
            }
            else if(renditionType.equalsIgnoreCase("o")){
                if(StringUtils.isBlank(asset.getPath())){
                    _logger.error("Original asset path is blank!");

                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                boolean canDownload = shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_DOWNLOAD, user.getId(), asset.getId(), true);

                if(!canDownload){
                    String errorMessage = "You do not have the permission to download this file!";
                    _logger.error(errorMessage);

                    return new ResponseEntity<>(HttpStatus.FORBIDDEN);
                }

                List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), asset.getId(), true);
                for(InternalShare internalShare: internalShares){
                    boolean downloadAllowed = internalShare.getCanDownload().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES);
                    boolean notifyOnDownload = internalShare.getNotifyOnDownload().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES);
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
                resource = new InputStreamResource(new FileInputStream(file));
            }

            return new ResponseEntity<>(resource, httpHeaders, HttpStatus.OK);
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the asset with ID " + assetId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}