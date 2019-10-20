package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.models.share.SharedAssets;
import com.github.murataykanat.toybox.models.share.SharedContainers;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
import com.github.murataykanat.toybox.schema.share.InternalShareRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ShareUtils {
    private static final Log _logger = LogFactory.getLog(ShareUtils.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private NotificationUtils notificationUtils;
    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private ContainerUtils containerUtils;
    @Autowired
    private AssetUtils assetUtils;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private InternalSharesRepository internalSharesRepository;
    @Autowired
    private InternalShareAssetsRepository internalShareAssetsRepository;
    @Autowired
    private InternalShareContainersRepository internalShareContainersRepository;
    @Autowired
    private InternalShareUsersRepository internalShareUsersRepository;

    @Autowired
    private ExternalSharesRepository externalSharesRepository;

    @LogEntryExitExecutionTime
    public ExternalShareResponse createExternalShare(User user, ExternalShareRequest externalShareRequest, SelectionContext selectionContext, HttpSession session) throws Exception {
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();

        String username = user.getUsername();
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
                        String message = "Asset '" + asset.getName() + "' is shared externally by '" + user.getUsername() + "'";

                        // Send notification for subscribers
                        List<User> subscribers = assetUtils.getSubscribers(asset.getId());
                        for(User subscriber: subscribers){
                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                            sendNotificationRequest.setFromUsername(user.getUsername());
                            sendNotificationRequest.setToUsername(subscriber.getUsername());
                            sendNotificationRequest.setMessage(message);
                            notificationUtils.sendNotification(sendNotificationRequest, session);
                        }

                        // Send notification for asset owners
                        List<InternalShare> internalShares = getInternalShares(user.getId(), asset.getId(), true);
                        for(InternalShare internalShare: internalShares){
                            if(internalShare.getNotifyOnShare().equalsIgnoreCase("Y")){
                                SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                sendNotificationRequest.setFromUsername(user.getUsername());
                                sendNotificationRequest.setToUsername(internalShare.getUsername());
                                sendNotificationRequest.setMessage(message);
                                notificationUtils.sendNotification(sendNotificationRequest, session);
                            }
                        }
                    }
                }

                if(selectedContainers != null && !selectedContainers.isEmpty()){
                    for(Container container: selectedContainers){
                        String message = "Folder '" + container.getName() + "' is shared externally by '" + user.getUsername() + "'";

                        // Send notification for subscribers
                        List<User> subscribers = containerUtils.getSubscribers(container.getId());
                        for(User subscriber: subscribers){
                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                            sendNotificationRequest.setFromUsername(user.getUsername());
                            sendNotificationRequest.setToUsername(subscriber.getUsername());
                            sendNotificationRequest.setMessage(message);
                            notificationUtils.sendNotification(sendNotificationRequest, session);
                        }

                        // Send notification for asset owners
                        List<InternalShare> internalShares = getInternalShares(user.getId(), container.getId(), false);
                        for(InternalShare internalShare: internalShares){
                            if(internalShare.getNotifyOnShare().equalsIgnoreCase("Y")){
                                SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                sendNotificationRequest.setFromUsername(user.getUsername());
                                sendNotificationRequest.setToUsername(internalShare.getUsername());
                                sendNotificationRequest.setMessage(message);
                                notificationUtils.sendNotification(sendNotificationRequest, session);
                            }
                        }
                    }
                }

                return externalShareResponse;
            }
            else{
                throw new IllegalArgumentException("Job response is null!");
            }
        }
        else{
            throw new IllegalArgumentException("Job response entity is null!");
        }
    }

    @LogEntryExitExecutionTime
    public void createInternalShare(User user, InternalShareRequest internalShareRequest, SelectionContext selectionContext, HttpSession session) throws Exception {
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

        String internalShareId = generateInternalShareId();

        Date expirationDate;

        if(!internalShareRequest.getEnableExpireInternal()){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
            expirationDate = simpleDateFormat.parse("12/31/9999 23:59:59");
        }
        else{
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(internalShareRequest.getExpirationDate());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            expirationDate = calendar.getTime();
        }

        String notifyOnEdit = internalShareRequest.getNotifyOnEdit() ? "Y" : "N";
        String notifyOnDownload = internalShareRequest.getNotifyOnDownload() ? "Y" : "N";
        String notifyOnShare = internalShareRequest.getNotifyOnShare() ? "Y" : "N";
        String notifyOnMoveOrCopy = internalShareRequest.getNotifyOnMoveOrCopy() ? "Y" : "N";
        String canEdit = internalShareRequest.getCanEdit() ? "Y" : "N";
        String canDownload = internalShareRequest.getCanDownload() ? "Y" : "N";
        String canShare = internalShareRequest.getCanShare() ? "Y" : "N";
        String canMoveOrCopy = internalShareRequest.getCanMoveOrCopy() ? "Y" : "N";


        internalSharesRepository.insertExternalShare(internalShareId, user.getUsername(), expirationDate,
                notifyOnEdit, notifyOnDownload, notifyOnShare, notifyOnMoveOrCopy, canEdit, canDownload, canShare, canMoveOrCopy);

        List<Asset> sharedAssets = new ArrayList<>();
        List<Container> sharedContainers = new ArrayList<>();

        for(User uniqueUser: uniqueUsers){
            List<InternalShareUser> internalShareUsersByInternalShareIdAndUserId = internalShareUsersRepository.findInternalShareUsersByInternalShareIdAndUserId(internalShareId, uniqueUser.getId());
            if(internalShareUsersByInternalShareIdAndUserId.isEmpty()){
                internalShareUsersRepository.insertShareUser(internalShareId, uniqueUser.getId());
            }
        }

        for(Asset selectedAsset: selectedAssets){
            List<InternalShareAsset> internalShareAssetByInternalShareIdAndAssetId = internalShareAssetsRepository.findInternalShareAssetByInternalShareIdAndAssetId(internalShareId, selectedAsset.getId());
            if(internalShareAssetByInternalShareIdAndAssetId.isEmpty()){
                internalShareAssetsRepository.insertSharedAsset(internalShareId, selectedAsset.getId());
                sharedAssets.add(selectedAsset);
            }
        }

        for(Container selectedContainer: selectedContainers){
            List<InternalShareContainer> internalShareContainersByInternalShareIdAndContainerId = internalShareContainersRepository.findInternalShareContainersByInternalShareIdAndContainerId(internalShareId, selectedContainer.getId());
            if(internalShareContainersByInternalShareIdAndContainerId.isEmpty()){
                internalShareContainersRepository.insertSharedContainer(internalShareId, selectedContainer.getId());
                sharedContainers.add(selectedContainer);

                List<Asset> containerAssets = containerUtils.getContainerAssets(selectedContainer.getId());
                for(Asset asset: containerAssets){
                    List<InternalShareAsset> internalShareAssetByInternalShareIdAndAssetId = internalShareAssetsRepository.findInternalShareAssetByInternalShareIdAndAssetId(internalShareId, asset.getId());
                    if(internalShareAssetByInternalShareIdAndAssetId.isEmpty()){
                        internalShareAssetsRepository.insertSharedAsset(internalShareId, asset.getId());
                        sharedAssets.add(asset);
                    }
                }
            }
        }

        if(!sharedAssets.isEmpty()){
            for(Asset asset: sharedAssets){
                // Send notification for subscribers
                String message = "Asset '" + asset.getName() + "' is shared internally by '" + user.getUsername() + "'";

                List<User> subscribers = assetUtils.getSubscribers(asset.getId());
                for(User subscriber: subscribers){
                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                    sendNotificationRequest.setFromUsername(user.getUsername());
                    sendNotificationRequest.setToUsername(subscriber.getUsername());
                    sendNotificationRequest.setMessage(message);
                    notificationUtils.sendNotification(sendNotificationRequest, session);
                }

                // Send notification for asset owners
                List<InternalShare> internalShares = getInternalShares(user.getId(), asset.getId(), true);
                for(InternalShare internalShare: internalShares){
                    if(internalShare.getNotifyOnShare().equalsIgnoreCase("Y")){
                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                        sendNotificationRequest.setFromUsername(user.getUsername());
                        sendNotificationRequest.setToUsername(internalShare.getUsername());
                        sendNotificationRequest.setMessage(message);
                        notificationUtils.sendNotification(sendNotificationRequest, session);
                    }
                }
            }
        }

        if(!sharedContainers.isEmpty()){
            for(Container container: sharedContainers){
                String message = "Folder '" + container.getName() + "' is shared internally by '" + user.getUsername() + "'";

                // Send notification for subscribers
                List<User> subscribers = containerUtils.getSubscribers(container.getId());
                for(User subscriber: subscribers){
                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                    sendNotificationRequest.setFromUsername(user.getUsername());
                    sendNotificationRequest.setToUsername(subscriber.getUsername());
                    sendNotificationRequest.setMessage(message);
                    notificationUtils.sendNotification(sendNotificationRequest, session);
                }

                // Send notification for asset owners
                List<InternalShare> internalShares = getInternalShares(user.getId(), container.getId(), false);
                for(InternalShare internalShare: internalShares){
                    if(internalShare.getNotifyOnShare().equalsIgnoreCase("Y")){
                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                        sendNotificationRequest.setFromUsername(user.getUsername());
                        sendNotificationRequest.setToUsername(internalShare.getUsername());
                        sendNotificationRequest.setMessage(message);
                        notificationUtils.sendNotification(sendNotificationRequest, session);
                    }
                }
            }
        }
    }

    @LogEntryExitExecutionTime
    public List<SharedAssets> getSharedAssets(int userId){
        List<SharedAssets> sharedAssetsLst = new ArrayList<>();

        // We find all the internal shares that were shared with the user
        List<InternalShareUser> internalShareUsersByUserId = internalShareUsersRepository.findInternalShareUsersByUserId(userId);
        // We iterate over the each internal share that was shared with the user
        for(InternalShareUser internalShareUser: internalShareUsersByUserId){
            // We find the internal share
            List<InternalShare> internalSharesById = internalSharesRepository.getInternalSharesById(internalShareUser.getInternalShareId());
            if(!internalSharesById.isEmpty()){
                if(internalSharesById.size() == 1){
                    // If the internal share exists and it is unique
                    InternalShare internalShare = internalSharesById.get(0);

                    // We find the assets that are linked to the internal share
                    List<InternalShareAsset> internalShareAssetByInternalShareId = internalShareAssetsRepository.findInternalShareAssetByInternalShareId(internalShare.getInternalShareId());
                    List<String> sharedAssetIds = internalShareAssetByInternalShareId.stream().map(InternalShareAsset::getAssetId).collect(Collectors.toList());

                    // We add the assets and the username who shared those assets to the shared asset list
                    SharedAssets sharedAssets = new SharedAssets();
                    sharedAssets.setUsername(internalShare.getUsername());
                    sharedAssets.setAssetIds(sharedAssetIds);

                    sharedAssetsLst.add(sharedAssets);
                }
                else{
                    throw new IllegalArgumentException("There are multiple instances of internal share ID '" + internalShareUser.getInternalShareId() + "' in the database!");
                }
            }
            else{
                throw new IllegalArgumentException("Internal share ID '" + internalShareUser.getInternalShareId() + "' does not exist in the database!");
            }
        }

        return sharedAssetsLst;
    }

    @LogEntryExitExecutionTime
    public boolean isSharedAsset(int userId, String assetId){
        List<SharedAssets> sharedAssetsLst = getSharedAssets(userId);
        for(SharedAssets sharedAssets: sharedAssetsLst){
            List<String> assetIds = sharedAssets.getAssetIds();
            boolean isShared = assetIds.stream().anyMatch(s -> s.equalsIgnoreCase(assetId));
            if(isShared){
                return true;
            }
        }

        return false;
    }

    public boolean isSharedContainer(int userId, String containerId){
        List<SharedContainers> sharedContainersLst = getSharedContainers(userId);
        for(SharedContainers sharedContainers: sharedContainersLst){
            List<String> containerIds = sharedContainers.getContainerIds();
            boolean isShared = containerIds.stream().anyMatch(s -> s.equalsIgnoreCase(containerId));
            if(isShared){
                return true;
            }
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public List<SharedContainers> getSharedContainers(int userId){
        List<SharedContainers> sharedContainersLst = new ArrayList<>();

        // We find all the internal shares that were shared with the user
        List<InternalShareUser> internalShareUsersByUserId = internalShareUsersRepository.findInternalShareUsersByUserId(userId);
        // We iterate over the each internal share that was shared with the user
        for(InternalShareUser internalShareUser: internalShareUsersByUserId){
            // We find the internal share
            List<InternalShare> internalSharesById = internalSharesRepository.getInternalSharesById(internalShareUser.getInternalShareId());
            if(!internalSharesById.isEmpty()){
                if(internalSharesById.size() == 1){
                    // If the internal share exists and it is unique
                    InternalShare internalShare = internalSharesById.get(0);

                    // We find the containers that are linked to the internal share
                    List<InternalShareContainer> internalShareContainersByInternalShareId = internalShareContainersRepository.findInternalShareContainersByInternalShareId(internalShare.getInternalShareId());
                    List<String> sharedContainerIds = internalShareContainersByInternalShareId.stream().map(InternalShareContainer::getContainerId).collect(Collectors.toList());

                    // We add the assets and the username who shared those assets to the shared asset list
                    SharedContainers sharedContainers = new SharedContainers();
                    sharedContainers.setUsername(internalShare.getUsername());
                    sharedContainers.setContainerIds(sharedContainerIds);

                    sharedContainersLst.add(sharedContainers);
                }
                else{
                    throw new IllegalArgumentException("There are multiple instances of internal share ID '" + internalShareUser.getInternalShareId() + "' in the database!");
                }
            }
            else{
                throw new IllegalArgumentException("Internal share ID '" + internalShareUser.getInternalShareId() + "' does not exist in the database!");
            }
        }

        return sharedContainersLst;
    }

    @LogEntryExitExecutionTime
    public List<InternalShare> getInternalShares(int userId, String id, boolean isAsset){
        List<InternalShare> internalShares = new ArrayList<>();

        // We find all the internal shares that were shared with the user
        List<InternalShareUser> internalShareUsersByUserId = internalShareUsersRepository.findInternalShareUsersByUserId(userId);
        // We iterate over the each internal share that was shared with the user
        for(InternalShareUser internalShareUser: internalShareUsersByUserId){
            // We find the internal share
            List<InternalShare> internalSharesById = internalSharesRepository.getInternalSharesById(internalShareUser.getInternalShareId());
            if(!internalSharesById.isEmpty()){
                if(internalSharesById.size() == 1){
                    // If the internal share exists and it is unique
                    InternalShare internalShare = internalSharesById.get(0);

                    if(isAsset){
                        // We find the assets that are linked to the external share
                        List<InternalShareAsset> internalShareAssetByInternalShareId = internalShareAssetsRepository.findInternalShareAssetByInternalShareId(internalShare.getInternalShareId());
                        List<String> sharedAssetIds = internalShareAssetByInternalShareId.stream().map(InternalShareAsset::getAssetId).collect(Collectors.toList());

                        boolean isAssetSharedWithUser = sharedAssetIds.stream().anyMatch(aid -> aid.equalsIgnoreCase(id));
                        if(isAssetSharedWithUser){
                            internalShares.add(internalShare);
                        }
                    }
                    else{
                        // We find the containers that are linked to the internal share
                        List<InternalShareContainer> internalShareContainersByInternalShareId = internalShareContainersRepository.findInternalShareContainersByInternalShareId(internalShare.getInternalShareId());
                        List<String> sharedContainerIds = internalShareContainersByInternalShareId.stream().map(InternalShareContainer::getContainerId).collect(Collectors.toList());

                        boolean isContainerSharedWithUser = sharedContainerIds.stream().anyMatch(cid -> cid.equalsIgnoreCase(id));
                        if(isContainerSharedWithUser){
                            internalShares.add(internalShare);
                        }
                    }
                }
                else{
                    throw new IllegalArgumentException("There are multiple instances of internal share ID '" + internalShareUser.getInternalShareId() + "' in the database!");
                }
            }
            else{
                throw new IllegalArgumentException("Internal share ID '" + internalShareUser.getInternalShareId() + "' does not exist in the database!");
            }
        }
        return internalShares;
    }

    @LogEntryExitExecutionTime
    private String generateInternalShareId(){
        String internalShareId = RandomStringUtils.randomAlphanumeric(40);
        if(isInternalShareIdValid(internalShareId)){
            return internalShareId;
        }
        return generateInternalShareId();
    }

    @LogEntryExitExecutionTime
    private boolean isInternalShareIdValid(String externalShareId){
        List<InternalShare> internalSharesById = internalSharesRepository.getInternalSharesById(externalShareId);
        return internalSharesById.isEmpty();
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
        return externalSharesById.isEmpty();
    }
}