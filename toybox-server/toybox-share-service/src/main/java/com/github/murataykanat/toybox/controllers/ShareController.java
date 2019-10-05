package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
import com.github.murataykanat.toybox.schema.share.InternalShareRequest;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.github.murataykanat.toybox.utilities.NotificationUtils;
import com.github.murataykanat.toybox.utilities.SelectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class ShareController {
    private static final Log _logger = LogFactory.getLog(ShareController.class);

    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private NotificationUtils notificationUtils;
    @Autowired
    private SelectionUtils selectionUtils;

    @Autowired
    private ExternalSharesRepository externalSharesRepository;
    @Autowired
    private UserAssetsRepository userAssetsRepository;
    @Autowired
    private UserContainersRepository userContainersRepository;
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/download/{externalShareId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadExternalShare(@PathVariable String externalShareId){
        try{
            if(StringUtils.isNotBlank(externalShareId)){
                List<ExternalShare> externalSharesById = externalSharesRepository.getExternalSharesById(externalShareId);
                if(!externalSharesById.isEmpty()){
                    if(externalSharesById.size() == 1){
                        ExternalShare externalShare = externalSharesById.get(0);

                        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

                        if(externalShare.getEnableExpireExternal().equalsIgnoreCase("Y")){
                            Date expirationDate = externalShare.getExpirationDate();
                            if(expirationDate != null){
                                Calendar cal = Calendar.getInstance();
                                cal.set(Calendar.HOUR_OF_DAY, 0);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                Date today = cal.getTime();

                                _logger.debug("Expiration date: " + formatter.format(expirationDate));
                                _logger.debug("Today: " + formatter.format(today));

                                if(!today.before(expirationDate)){
                                    String errorMessage = "The external share is expired!";
                                    _logger.error(errorMessage);

                                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                                }
                            }
                            else{
                                throw new IllegalArgumentException("Expiration date is null!");
                            }
                        }

                        if(externalShare.getEnableUsageLimit().equalsIgnoreCase("Y")){
                            int maxNumberOfHits = externalShare.getMaxNumberOfHits();
                            if(maxNumberOfHits <= 0){
                                String errorMessage = "The maximum number of uses exceeded the set amount!";
                                _logger.error(errorMessage);

                                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                            }
                        }

                        String downloadFilePath =  exportStagingPath + File.separator + externalShare.getJobId() + File.separator + "Download.zip";

                        File archiveFile = new File(downloadFilePath);
                        if(archiveFile.exists()){
                            if(externalShare.getNotifyWhenDownloaded().equalsIgnoreCase("Y")){
                                // Send notification manually (because we don't have a session)
                                String toUsername = externalShare.getUsername();
                                String fromUsername = "system";

                                Notification notification = new Notification();
                                notification.setUsername(toUsername);
                                notification.setNotification("The assets you externally shared were downloaded.");
                                notification.setIsRead("N");
                                notification.setDate(new Date());
                                notification.setFrom(fromUsername);

                                rabbitTemplate.convertAndSend(ToyboxConstants.TOYBOX_NOTIFICATION_EXCHANGE,"toybox.notification." + System.currentTimeMillis(), notification);
                            }

                            if(externalShare.getEnableUsageLimit().equalsIgnoreCase("Y")){
                                int maxNumberOfHits = externalShare.getMaxNumberOfHits();
                                maxNumberOfHits--;
                                externalSharesRepository.updateMaxUsage(maxNumberOfHits, externalShareId);
                            }

                            InputStreamResource resource = new InputStreamResource(new FileInputStream(archiveFile));

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Content-disposition", "attachment; filename=Download.zip");
                            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                        }
                        else{
                            throw new IOException("File path '" + downloadFilePath + "' is not valid!");
                        }
                    }
                    else{
                        throw new IllegalArgumentException("There are multiple external shares with ID '" + externalShareId + "'!");
                    }
                }
                else{
                    String errorMessage = "External share ID is not found! ";
                    _logger.error(errorMessage);

                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "External share ID is blank! ";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading the shared assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/external", method = RequestMethod.POST)
    public ResponseEntity<ExternalShareResponse> createExternalShare(Authentication authentication, HttpSession session, @RequestBody ExternalShareRequest externalShareRequest) {
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    if(externalShareRequest != null){
                        SelectionContext selectionContext = externalShareRequest.getSelectionContext();
                        if(selectionUtils.isSelectionContextValid(selectionContext)){
                            String username = authentication.getName();
                            Date expirationDate = externalShareRequest.getExpirationDate();
                            int maxNumberOfHits = externalShareRequest.getMaxNumberOfHits();
                            String notifyWhenDownloaded = externalShareRequest.getNotifyWhenDownloaded() ? "Y" : "N";
                            String enableExpireExternal = externalShareRequest.getEnableExpireExternal() ? "Y" : "N";
                            String enableUsageLimit = externalShareRequest.getEnableUsageLimit() ? "Y" : "N";

                            if(!externalShareRequest.getEnableExpireExternal()){
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
                                expirationDate = simpleDateFormat.parse("12/31/9999 23:59:59");
                            }
                            else{
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(expirationDate);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                expirationDate = calendar.getTime();
                            }

                            RestTemplate restTemplate = new RestTemplate();
                            HttpHeaders headers = authenticationUtils.getHeaders(session);

                            HttpEntity<SelectionContext> selectionContextEntity = new HttpEntity<>(selectionContext, headers);
                            String jobServiceUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.JOB_SERVICE_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.JOB_SERVICE_NAME, session, false);
                            String shareServiceUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.SHARE_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.SHARE_SERVICE_NAME, session, false);

                            ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/package", selectionContextEntity, JobResponse.class);
                            if(jobResponseResponseEntity != null){
                                JobResponse jobResponse = jobResponseResponseEntity.getBody();
                                if(jobResponse != null){
                                    _logger.debug("Job response message: " + jobResponse.getMessage());
                                    _logger.debug("Job ID: " + jobResponse.getJobId());

                                    String externalShareId = generateExternalShareId();

                                    externalSharesRepository.insertExternalShare(externalShareId, username, jobResponse.getJobId(), expirationDate, maxNumberOfHits, notifyWhenDownloaded, enableExpireExternal, enableUsageLimit);

                                    externalShareResponse.setMessage("External share successfully generated.");
                                    externalShareResponse.setUrl(shareServiceUrl + "/share/download/" + externalShareId);

                                    List<Asset> selectedAssets = externalShareRequest.getSelectionContext().getSelectedAssets();
                                    List<Container> selectedContainers = externalShareRequest.getSelectionContext().getSelectedContainers();

                                    if(selectedAssets != null && !selectedAssets.isEmpty()){
                                        for(Asset asset: selectedAssets){
                                            // Send notification
                                            String message = "Asset '" + asset.getName() + "' is shared externally by '" + user.getUsername() + "'";
                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                            sendNotificationRequest.setIsAsset(true);
                                            sendNotificationRequest.setId(asset.getId());
                                            sendNotificationRequest.setFromUser(user);
                                            sendNotificationRequest.setMessage(message);
                                            notificationUtils.sendNotification(sendNotificationRequest, session);
                                        }
                                    }

                                    if(selectedContainers != null && !selectedContainers.isEmpty()){
                                        for(Container container: selectedContainers){
                                            // Send notification
                                            String message = "Folder '" + container.getName() + "' is shared externally by '" + user.getUsername() + "'";
                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                            sendNotificationRequest.setIsAsset(false);
                                            sendNotificationRequest.setId(container.getId());
                                            sendNotificationRequest.setFromUser(user);
                                            sendNotificationRequest.setMessage(message);
                                            notificationUtils.sendNotification(sendNotificationRequest, session);
                                        }
                                    }

                                    return new ResponseEntity<>(externalShareResponse, HttpStatus.CREATED);
                                }
                                else{
                                    throw new IllegalArgumentException("Job response is null!");
                                }
                            }
                            else{
                                throw new IllegalArgumentException("Job response entity is null!");
                            }
                        }
                        else{
                            String errorMessage = "Selection context is not valid!";
                            _logger.error(errorMessage);

                            externalShareResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(externalShareResponse, HttpStatus.BAD_REQUEST);
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
                    throw new IllegalArgumentException("User is null!");
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
    @RequestMapping(value = "/share/internal", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> createInternalShare(Authentication authentication, HttpSession session, @RequestBody InternalShareRequest internalShareRequest) {
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    if(internalShareRequest != null){
                        SelectionContext selectionContext = internalShareRequest.getSelectionContext();
                        if(selectionUtils.isSelectionContextValid(selectionContext)){
                            List<Asset> selectedAssets = selectionContext.getSelectedAssets();
                            List<Container> selectedContainers = selectionContext.getSelectedContainers();
                            List<String> sharedUsergroupNames = internalShareRequest.getSharedUsergroups();
                            List<String> sharedUserNames = internalShareRequest.getSharedUsers();

                            // We find all the users that are in the shared user groups
                            List<User> sharedUsers = new ArrayList<>();
                            for(String sharedUsergroupName: sharedUsergroupNames){
                                List<User> usersInUserGroup = authenticationUtils.getUsersInUserGroup(sharedUsergroupName);
                                // We exclude the current user
                                List<User> usersExcludingCurrentUser = usersInUserGroup.stream().filter(u -> !u.getUsername().equalsIgnoreCase(user.getUsername())).collect(Collectors.toList());
                                sharedUsers.addAll(usersExcludingCurrentUser);
                            }
                            // We find the shared users
                            if(!sharedUserNames.isEmpty()){
                                List<User> usersByUsernames = usersRepository.findUsersByUsernames(sharedUserNames);
                                List<User> usersExcludingCurrentUser = usersByUsernames.stream().filter(u -> !u.getUsername().equalsIgnoreCase(user.getUsername())).collect(Collectors.toList());
                                sharedUsers.addAll(usersExcludingCurrentUser);
                            }

                            // We create the unique users list to share
                            List<User> uniqueUsers = new ArrayList<>(new HashSet<>(sharedUsers));


                            // We set the containers and users as shared with all the unique users
                            List<Asset> sharedAssets = new ArrayList<>();
                            List<Container> sharedContainers = new ArrayList<>();
                            for(User uniqueUser: uniqueUsers){
                                for(Asset selectedAsset: selectedAssets){
                                    List<UserAsset> userAssetsByUserIdAndAssetId = userAssetsRepository.findUserAssetsByUserIdAndAssetId(uniqueUser.getId(), selectedAsset.getId());
                                    if(userAssetsByUserIdAndAssetId.isEmpty()){
                                        userAssetsRepository.insertSharedAsset(selectedAsset.getId(), uniqueUser.getId());
                                        sharedAssets.add(selectedAsset);
                                    }
                                }
                                for(Container selectedContainer: selectedContainers){
                                    List<UserContainer> userAssetsByUserIdAndContainerId = userContainersRepository.findUserAssetsByUserIdAndContainerId(uniqueUser.getId(), selectedContainer.getId());
                                    if(userAssetsByUserIdAndContainerId.isEmpty()){
                                        userContainersRepository.insertSharedContainer(selectedContainer.getId(), uniqueUser.getId());
                                        sharedContainers.add(selectedContainer);
                                    }
                                }
                            }

                            if(!sharedAssets.isEmpty()){
                                for(Asset asset: sharedAssets){
                                    // Send notification
                                    String message = "Asset '" + asset.getName() + "' is shared internally by '" + user.getUsername() + "'";
                                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                    sendNotificationRequest.setIsAsset(true);
                                    sendNotificationRequest.setId(asset.getId());
                                    sendNotificationRequest.setFromUser(user);
                                    sendNotificationRequest.setMessage(message);
                                    notificationUtils.sendNotification(sendNotificationRequest, session);
                                }
                            }

                            if(!sharedContainers.isEmpty()){
                                for(Container container: sharedContainers){
                                    // Send notification
                                    String message = "Folder '" + container.getName() + "' is shared internally by '" + user.getUsername() + "'";
                                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                    sendNotificationRequest.setIsAsset(false);
                                    sendNotificationRequest.setId(container.getId());
                                    sendNotificationRequest.setFromUser(user);
                                    sendNotificationRequest.setMessage(message);
                                    notificationUtils.sendNotification(sendNotificationRequest, session);
                                }
                            }

                            if(selectedAssets.size() == sharedAssets.size() && selectedContainers.size() == sharedContainers.size()){
                                genericResponse.setMessage("Internal share created successfully!");
                                return new ResponseEntity<>(genericResponse, HttpStatus.CREATED);
                            }
                            else{
                                genericResponse.setMessage("Internal share created successfully. Some of the selected assets/folders were already shared with the selected users. No action was taken on those items.");
                                return new ResponseEntity<>(genericResponse, HttpStatus.CREATED);
                            }
                        }
                        else{
                            String errorMessage = "Selection context is not valid!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                        }
                    }
                    else {
                        String errorMessage = "Internal share request is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    throw new IllegalArgumentException("User is null!");
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
            String errorMessage = "An error occurred while sharing assets externally. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    private String generateExternalShareId(){
        String externalShareId = RandomStringUtils.randomAlphanumeric(40);
        if(isExternalShareIdValid(externalShareId)){
            return externalShareId;
        }
        return generateExternalShareId();
    }

    @LogEntryExitExecutionTime
    private boolean isExternalShareIdValid(String externalShareId){
        List<ExternalShare> externalSharesById = externalSharesRepository.getExternalSharesById(externalShareId);
        if(externalSharesById.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }
}