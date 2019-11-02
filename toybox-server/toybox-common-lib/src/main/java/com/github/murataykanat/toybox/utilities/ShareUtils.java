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
                        List<InternalShare> internalShares = getInternalSharesWithTargetUser(user.getId(), asset.getId(), true);
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
                        List<InternalShare> internalShares = getInternalSharesWithTargetUser(user.getId(), container.getId(), false);
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
        List<String> sharedUsernames = internalShareRequest.getSharedUsers();

        // We find all the users that are in the shared user groups
        List<User> sharedUsers = new ArrayList<>();
        List<User> usersToIgnore = new ArrayList<>();

        usersToIgnore.add(user);

        for(Asset selectedAsset: selectedAssets){
            if(selectedAsset.getShared().equalsIgnoreCase("Y")){
                usersToIgnore.add(authenticationUtils.getUser(selectedAsset.getSharedByUsername()));
            }
        }

        for(Container selectedContainer: selectedContainers){
            if(selectedContainer.getShared().equalsIgnoreCase("Y")){
                usersToIgnore.add(authenticationUtils.getUser(selectedContainer.getSharedByUsername()));
            }
        }

        for(String sharedUsergroupName: sharedUsergroupNames){
            List<User> usersInUserGroup = authenticationUtils.getUsersInUserGroup(sharedUsergroupName);
            for(User userInUserGroup: usersInUserGroup){
                boolean ignoreUser = false;
                for(User userToIgnore: usersToIgnore){
                    if(userInUserGroup.getId() == userToIgnore.getId()){
                        ignoreUser = true;
                        break;
                    }
                }

                if(!ignoreUser){
                    sharedUsers.add(userInUserGroup);
                }
            }
        }

        // We find the shared users
        if(!sharedUsernames.isEmpty()){
            List<User> usersByUsernames = usersRepository.findUsersByUsernames(sharedUsernames);
            List<User> usersToShare = usersByUsernames.stream().filter(u -> !u.getUsername().equalsIgnoreCase(user.getUsername())).collect(Collectors.toList());
            sharedUsers.addAll(usersToShare);
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
                List<Container> containersToAdd = containerUtils.getSubContainerTree(new ArrayList<>(), selectedContainer.getId());
                containersToAdd.add(selectedContainer);

                for (Container containerToAdd: containersToAdd){
                    internalShareContainersRepository.insertSharedContainer(internalShareId, containerToAdd.getId());
                    sharedContainers.add(containerToAdd);

                    List<Asset> containerAssets = containerUtils.getContainerAssets(containerToAdd.getId());

                    for(Asset asset: containerAssets){
                        List<InternalShareAsset> internalShareAssetByInternalShareIdAndAssetId = internalShareAssetsRepository.findInternalShareAssetByInternalShareIdAndAssetId(internalShareId, asset.getId());
                        if(internalShareAssetByInternalShareIdAndAssetId.isEmpty()){
                            internalShareAssetsRepository.insertSharedAsset(internalShareId, asset.getId());
                            sharedAssets.add(asset);
                        }
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
                List<InternalShare> internalShares = getInternalSharesWithTargetUser(user.getId(), asset.getId(), true);
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
                List<InternalShare> internalShares = getInternalSharesWithTargetUser(user.getId(), container.getId(), false);
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
    public User getSourceUser(int targetUserId, String id, boolean isAsset) throws Exception {
        List<InternalShareUser> internalShareUsersByUserId = internalShareUsersRepository.findInternalShareUsersByUserId(targetUserId);
        for(InternalShareUser internalShareUser: internalShareUsersByUserId){
            InternalShareItem match;

            if(isAsset){
                List<InternalShareAsset> internalShareAssetByInternalShareId = internalShareAssetsRepository.findInternalShareAssetByInternalShareId(internalShareUser.getInternalShareId());
                match = internalShareAssetByInternalShareId.stream().filter(internalShareAsset -> internalShareAsset.getAssetId().equalsIgnoreCase(id)).findFirst().orElse(null);

            }
            else{
                List<InternalShareContainer> internalShareContainersByInternalShareId = internalShareContainersRepository.findInternalShareContainersByInternalShareId(internalShareUser.getInternalShareId());
                match = internalShareContainersByInternalShareId.stream().filter(internalShareContainer -> internalShareContainer.getContainerId().equalsIgnoreCase(id)).findFirst().orElse(null);
            }

            if(match != null){
                List<InternalShare> internalSharesById = internalSharesRepository.getInternalSharesById(match.getInternalShareId());
                if(!internalSharesById.isEmpty()){
                    if(internalSharesById.size() == 1){
                        InternalShare internalShare = internalSharesById.get(0);
                        return authenticationUtils.getUser(internalShare.getUsername());
                    }
                    else{
                        throw new IllegalArgumentException("There are multiple instances of internal share ID '" + internalShareUser.getInternalShareId() + "' in the database!");
                    }
                }
                else{
                    throw new IllegalArgumentException("Internal share ID '" + internalShareUser.getInternalShareId() + "' does not exist in the database!");
                }
            }
            else{
                throw new IllegalArgumentException("Match is null!");
            }
        }

        return null;
    }

    @LogEntryExitExecutionTime
    public boolean isAssetSharedWithUser(int userId, String assetId){
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

    @LogEntryExitExecutionTime
    public boolean isContainerSharedWithUser(int userId, String containerId){
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
    public List<InternalShare> getInternalSharesContainingItem(String id, boolean isAsset){
        List<InternalShare> internalShares = new ArrayList<>();
        List<String> internalShareIds;

        if(isAsset){
            List<InternalShareAsset> internalShareAssetByAssetId = internalShareAssetsRepository.findInternalShareAssetByAssetId(id);
            internalShareIds = internalShareAssetByAssetId.stream().map(InternalShareAsset::getInternalShareId).collect(Collectors.toList());
        }
        else{
            List<InternalShareContainer> internalShareContainersByContainerId = internalShareContainersRepository.findInternalShareContainersByContainerId(id);
            internalShareIds = internalShareContainersByContainerId.stream().map(InternalShareContainer::getInternalShareId).collect(Collectors.toList());
        }

        if(!internalShareIds.isEmpty()){
            internalShares = internalSharesRepository.getInternalSharesByIds(internalShareIds);
        }

        return internalShares;
    }

    @LogEntryExitExecutionTime
    public List<InternalShare> getInternalSharesWithSourceUser(int sourceUserId, String id, boolean isAsset){
        List<InternalShare> internalShares = new ArrayList<>();

        User user = authenticationUtils.getUser(sourceUserId);
        List<InternalShare> internalSharesByUsername = internalSharesRepository.getInternalSharesByUsername(user.getUsername());
        for(InternalShare internalShare: internalSharesByUsername){
            if(isAsset){
                List<InternalShareAsset> internalShareAssetByInternalShareId = internalShareAssetsRepository.findInternalShareAssetByInternalShareId(internalShare.getInternalShareId());
                boolean isMatch = internalShareAssetByInternalShareId.stream().anyMatch(internalShareAsset -> internalShareAsset.getAssetId().equalsIgnoreCase(id));
                if(isMatch){
                    internalShares.add(internalShare);
                }
            }
            else{
                List<InternalShareContainer> internalShareContainersByContainerId = internalShareContainersRepository.findInternalShareContainersByContainerId(id);
                boolean isMatch = internalShareContainersByContainerId.stream().anyMatch(internalShareContainer -> internalShareContainer.getContainerId().equalsIgnoreCase(id));
                if(isMatch){
                    internalShares.add(internalShare);
                }
            }
        }

        return internalShares;
    }

    @LogEntryExitExecutionTime
    public List<InternalShare> getInternalSharesWithTargetUser(int targetUserId, String id, boolean isAsset){
        List<InternalShare> internalShares = new ArrayList<>();

        // We find all the internal shares that were shared with the user
        List<InternalShareUser> internalShareUsersByUserId = internalShareUsersRepository.findInternalShareUsersByUserId(targetUserId);
        // We iterate over the each internal share that was shared with the user
        for(InternalShareUser internalShareUser: internalShareUsersByUserId){
            // We find the internal share
            List<InternalShare> internalSharesById = internalSharesRepository.getInternalSharesById(internalShareUser.getInternalShareId());
            if(!internalSharesById.isEmpty()){
                if(internalSharesById.size() == 1){
                    // If the internal share exists and it is unique
                    InternalShare internalShare = internalSharesById.get(0);

                    if(isAsset){
                        // We check if the asset is shared with the user
                        if(isAssetSharedWithUser(targetUserId, id)){
                            internalShares.add(internalShare);
                        }
                    }
                    else{
                        // We check if the container is shared with the user
                        if(isContainerSharedWithUser(targetUserId, id)){
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

    public void removeItemFromInternalShare(String internalShareId, String id, boolean isAsset){
        if(isAsset){
            internalShareAssetsRepository.deleteSharedAssetByInternalShareIdAndAssetId(internalShareId, id);
        }
        else{
            internalShareContainersRepository.deleteSharedContainerByInternalShareIdAndContainerId(internalShareId, id);
            List<Container> subContainerTree = containerUtils.getSubContainerTree(new ArrayList<>(), id);

            for(Container container: subContainerTree){
                internalShareContainersRepository.deleteSharedContainerByInternalShareIdAndContainerId(internalShareId, container.getId());

                List<Asset> containerAssets = containerUtils.getContainerAssets(container.getId());
                for(Asset asset: containerAssets){
                    internalShareAssetsRepository.deleteSharedAssetByInternalShareIdAndAssetId(internalShareId, asset.getId());
                }
            }
        }
    }

    @LogEntryExitExecutionTime
    public void updateInternalShareAsset(String newAssetId, String oldAssetId){
        internalShareAssetsRepository.updateSharedAssets(newAssetId, oldAssetId);
    }

    @LogEntryExitExecutionTime
    public void addAssetToInternalShare(String assetId, String shareId){
        List<InternalShareAsset> internalShareAssetByInternalShareIdAndAssetId = internalShareAssetsRepository.findInternalShareAssetByInternalShareIdAndAssetId(shareId, assetId);
        if(internalShareAssetByInternalShareIdAndAssetId.isEmpty()){
            internalShareAssetsRepository.insertSharedAsset(shareId, assetId);
        }
    }

    @LogEntryExitExecutionTime
    public void addContainerToInternalShare(String containerId, String shareId){
        List<InternalShareContainer> internalShareContainersByInternalShareIdAndContainerId = internalShareContainersRepository.findInternalShareContainersByInternalShareIdAndContainerId(shareId, containerId);
        if(internalShareContainersByInternalShareIdAndContainerId.isEmpty()){
            internalShareContainersRepository.insertSharedContainer(shareId, containerId);
        }
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