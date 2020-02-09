package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.predicates.AssetPredicateBuilder;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.asset.UpdateAssetRequest;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AssetUtils {
    private static Log _logger = LogFactory.getLog(AssetUtils.class);

    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private NotificationUtils notificationUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private SortUtils sortUtils;
    @Autowired
    private ContainerUtils containerUtils;
    @Autowired
    private ShareUtils shareUtils;

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private AssetUserRepository assetUserRepository;
    @Autowired
    private ContainersRepository containersRepository;
    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;

    @LogEntryExitExecutionTime
    public List<Asset> getAssets(List<SearchCondition> searchConditions){
        if(searchConditions == null || searchConditions.isEmpty()){
            return assetsRepository.findAll();
        }
        else{
            AssetPredicateBuilder builder = new AssetPredicateBuilder().with(searchConditions);
            Iterable<Asset> iterableAssets = assetsRepository.findAll(builder.build());
            return Lists.newArrayList(iterableAssets);
        }
    }

    @LogEntryExitExecutionTime
    public Asset getAsset(String assetId) {
        List<Asset> assetsById = assetsRepository.getAssetsById(assetId);
        if(!assetsById.isEmpty()){
            if(assetsById.size() == 1){
                return assetsById.get(0);
            }
            else{
                throw new IllegalArgumentException("There are multiple assets with ID '" + assetId + "'!");
            }
        }
        else{
            throw new IllegalArgumentException("There is no asset with ID '" + assetId + "'!");
        }
    }

    public List<Asset> getAssetsByAssetIds(List<String> assetIds){
        return assetsRepository.getAssetsByAssetIds(assetIds);
    }

    @LogEntryExitExecutionTime
    public boolean isSubscribed(User user, Asset asset){
        List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());
        for(AssetUser assetUser: assetUsersByUserId){
            if(assetUser.getAssetId().equalsIgnoreCase(asset.getId())){
                return true;
            }
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public int moveAssets(List<String> assetIds, Container targetContainer, User user, HttpSession session) throws Exception {
        int numberOfIgnoredFiles = 0;

        for(String assetId: assetIds){
            boolean assetAlreadyInTargetContainer = false;
            Asset asset = getAsset(assetId);

            Asset duplicateAsset = null;
            List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(targetContainer.getId());
            for(ContainerAsset containerAsset: containerAssetsByContainerId){
                Asset assetInContainer = getAsset(containerAsset.getAssetId());
                if(assetInContainer.getId().equalsIgnoreCase(asset.getId())){
                    assetAlreadyInTargetContainer = true;
                    break;
                }
                if(assetInContainer.getName().equalsIgnoreCase(asset.getName()) && assetInContainer.getIsLatestVersion().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES) && assetInContainer.getDeleted().equalsIgnoreCase(ToyboxConstants.LOOKUP_NO)){
                    duplicateAsset = assetInContainer;
                    break;
                }
            }

            if(!assetAlreadyInTargetContainer){
                if(duplicateAsset != null){
                    // Container has a duplicate asset
                    // We must add the new asset and its versions as new versions of the duplicate asset.
                    List<Asset> duplicateAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(duplicateAsset.getOriginalAssetId());
                    assetsRepository.updateAssetsLatestVersion(ToyboxConstants.LOOKUP_NO, duplicateAssetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));

                    List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                    containerAssetsRepository.moveAssets(targetContainer.getId(), assetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));

                    assetsRepository.updateAssetsOriginalAssetId(duplicateAsset.getOriginalAssetId(), assetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));

                    int latestVersionOfDuplicate = duplicateAsset.getVersion();

                    sortUtils.sortItems("asc", assetsByOriginalAssetId, Comparator.comparing(Asset::getVersion));
                    for(Asset movedAsset : assetsByOriginalAssetId){
                        latestVersionOfDuplicate++;
                        assetsRepository.updateAssetVersion(latestVersionOfDuplicate, movedAsset.getId());
                    }
                }
                else{
                    // Container does not have duplicate assets
                    List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                    containerAssetsRepository.moveAssets(targetContainer.getId(), assetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));
                }

                String message = "Asset '" + asset.getName() + "' is moved to folder '" + targetContainer.getName() + "' by '" + user.getUsername() + "'";

                // Send notification for subscribers
                List<User> subscribers = getSubscribers(asset.getId());
                for(User subscriber: subscribers){
                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                    sendNotificationRequest.setFromUsername(user.getUsername());
                    sendNotificationRequest.setToUsername(subscriber.getUsername());
                    sendNotificationRequest.setMessage(message);
                    notificationUtils.sendNotification(sendNotificationRequest, session);
                }
            }
            else{
                numberOfIgnoredFiles++;
                _logger.debug("Asset with ID '" + asset.getId() + " is already in the container with ID '" + targetContainer.getId() + "'. Skipping move...");
            }
        }

        return numberOfIgnoredFiles;
    }

    @LogEntryExitExecutionTime
    public int copyAssets(HttpSession session, List<String> sourceAssetIds, List<String> targetContainerIds, String username, String importStagingPath) throws Exception {
        int numberOfFailedJobs = 0;

        for(String targetContainerId : targetContainerIds){
            numberOfFailedJobs += duplicateAssets(session, sourceAssetIds, importStagingPath, targetContainerId, username);
        }

        return numberOfFailedJobs;
    }

    @LogEntryExitExecutionTime
    private int duplicateAssets(HttpSession session, List<String> assetIds, String importStagingPath, String targetContainerId, String username) throws Exception {
        int numberOfFailedAssets = 0;

        UploadFileLst uploadFileLst = new UploadFileLst();
        uploadFileLst.setContainerId(targetContainerId);

        Container targetContainer = containerUtils.getContainer(targetContainerId);
        List<Asset> copiedAssets = new ArrayList<>();
        User user = authenticationUtils.getUser(username);

        List<UploadFile> uploadFiles = new ArrayList<>();
        for(String assetId: assetIds){
            Asset asset = getAsset(assetId);
            copiedAssets.add(asset);

            File currentFile = new File(asset.getPath());

            String tempFolderName = Long.toString(System.currentTimeMillis());
            String tempImportStagingPath = importStagingPath + File.separator + tempFolderName;
            _logger.debug("Import staging path: " + tempImportStagingPath);

            File tempFolder = new File(tempImportStagingPath);
            if(!tempFolder.exists()){
                if(!tempFolder.mkdir()){
                    throw new IOException("Unable to generate the temp folder path '" + tempImportStagingPath + "'!");
                }
            }

            String destinationPath;
            if(tempFolder.exists()){
                destinationPath = tempFolder.getAbsolutePath() + File.separator + asset.getName();
            }
            else{
                throw new FileNotFoundException("The temp folder '" + tempFolder.getAbsolutePath() + "' does not exist!");
            }

            Files.copy(currentFile.toPath(), new File(destinationPath).toPath());

            UploadFile uploadFile = new UploadFile();
            uploadFile.setUsername(username);
            uploadFile.setPath(destinationPath);

            uploadFiles.add(uploadFile);
        }

        uploadFileLst.setUploadFiles(uploadFiles);

        String jobServiceUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.JOB_SERVICE_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.JOB_SERVICE_NAME, session, false);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = authenticationUtils.getHeaders(session);

        HttpEntity<UploadFileLst> uploadFileLstEntity = new HttpEntity<>(uploadFileLst, headers);
        ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/import", uploadFileLstEntity, JobResponse.class);
        boolean successful = jobResponseResponseEntity.getStatusCode().is2xxSuccessful();

        if(successful){
            _logger.debug("Asset import job successfully started!");
            for(Asset copiedAsset: copiedAssets){
                String message = "Asset '" + copiedAsset.getName() + "' is copied to folder '" + targetContainer.getName() + "' by '" + user.getUsername() + "'";

                // Send notification for subscribers
                List<User> subscribers = getSubscribers(copiedAsset.getId());
                for(User subscriber: subscribers){
                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                    sendNotificationRequest.setFromUsername(user.getUsername());
                    sendNotificationRequest.setToUsername(subscriber.getUsername());
                    sendNotificationRequest.setMessage(message);
                    notificationUtils.sendNotification(sendNotificationRequest, session);
                }

                // Send notification for asset owners
                List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), copiedAsset.getId(), true);
                for(InternalShare internalShare: internalShares){
                    if(internalShare.getNotifyOnCopy().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES)){
                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                        sendNotificationRequest.setFromUsername(user.getUsername());
                        sendNotificationRequest.setToUsername(internalShare.getUsername());
                        sendNotificationRequest.setMessage(message);
                        notificationUtils.sendNotification(sendNotificationRequest, session);
                    }
                }
            }
        }
        else{
            _logger.error("Asset import job failed to start. " + jobResponseResponseEntity.getBody().getMessage());
            numberOfFailedAssets += uploadFileLst.getUploadFiles().size();
        }

        return numberOfFailedAssets;
    }

    @LogEntryExitExecutionTime
    public Asset updateAsset(UpdateAssetRequest updateAssetRequest, String assetId, User user, HttpSession session) throws Exception {
        Asset asset = getAsset(assetId);
        boolean updateChecksum = StringUtils.isNotBlank(updateAssetRequest.getChecksum());
        boolean updateDeleted = StringUtils.isNotBlank(updateAssetRequest.getDeleted());
        boolean updateExtension = StringUtils.isNotBlank(updateAssetRequest.getExtension());
        boolean updateFileSize = StringUtils.isNotBlank(updateAssetRequest.getFileSize());
        boolean updateImportDate = updateAssetRequest.getImportDate() != null;
        boolean updateImportedByUsername = StringUtils.isNotBlank(updateAssetRequest.getImportedByUsername());
        boolean updateIsLatestVersion = StringUtils.isNotBlank(updateAssetRequest.getIsLatestVersion());
        boolean updateName = StringUtils.isNotBlank(updateAssetRequest.getName());
        boolean updateOriginalAssetId = StringUtils.isNotBlank(updateAssetRequest.getOriginalAssetId());
        boolean updatePath = StringUtils.isNotBlank(updateAssetRequest.getPath());
        boolean updatePreviewPath = StringUtils.isNotBlank(updateAssetRequest.getPreviewPath());
        boolean updateThumbnailPath = StringUtils.isNotBlank(updateAssetRequest.getThumbnailPath());
        boolean updateType = StringUtils.isNotBlank(updateAssetRequest.getType());
        boolean updateVersion = updateAssetRequest.getVersion() != 0;

        asset.setChecksum(updateChecksum ? updateAssetRequest.getChecksum() : asset.getChecksum());
        asset.setDeleted(updateDeleted ? updateAssetRequest.getDeleted() : asset.getDeleted());
        asset.setExtension(updateExtension ? updateAssetRequest.getExtension() : asset.getExtension());
        asset.setFileSize(updateFileSize ? updateAssetRequest.getFileSize() : asset.getFileSize());
        asset.setImportDate(updateImportDate ? updateAssetRequest.getImportDate() : asset.getImportDate());
        asset.setImportedByUsername(updateImportedByUsername ? updateAssetRequest.getImportedByUsername() : asset.getImportedByUsername());
        asset.setIsLatestVersion(updateIsLatestVersion ? updateAssetRequest.getIsLatestVersion() : asset.getIsLatestVersion());
        asset.setName(updateName ? updateAssetRequest.getName() : asset.getName());
        asset.setOriginalAssetId(updateOriginalAssetId ? updateAssetRequest.getOriginalAssetId() : asset.getOriginalAssetId());
        asset.setPath(updatePath ? updateAssetRequest.getPath() : asset.getPath());
        asset.setPreviewPath(updatePreviewPath ? updateAssetRequest.getPreviewPath() : asset.getPreviewPath());
        asset.setThumbnailPath(updateThumbnailPath ? updateAssetRequest.getThumbnailPath() : asset.getThumbnailPath());
        asset.setType(updateType ? updateAssetRequest.getType() : asset.getType());
        asset.setVersion(updateVersion ? updateAssetRequest.getVersion() : asset.getVersion());
        assetsRepository.save(asset);

        if(updateName){
            List<Asset> assetsByOriginalAssetId = assetsRepository.getAssetsByOriginalAssetId(asset.getOriginalAssetId());
            if(!assetsByOriginalAssetId.isEmpty()){
                for(Asset versionedAsset: assetsByOriginalAssetId){
                    String extension = versionedAsset.getExtension().toLowerCase();
                    String newFileName = updateAssetRequest.getName() + "." + extension;
                    File oldFile = new File(versionedAsset.getPath());
                    if(oldFile.exists()){
                        String parentDirectoryPath = oldFile.getParentFile().getAbsolutePath();
                        String newFilePath = parentDirectoryPath + File.separator + newFileName;
                        File newFile = new File(newFilePath);
                        FileUtils.moveFile(oldFile, newFile);

                        assetsRepository.updateAssetName(newFileName, newFilePath, versionedAsset.getId());
                    }
                    else{
                        throw new IOException("File path " + versionedAsset.getPath() + " is not a valid file!");
                    }
                }
            }
            else{
                throw new IllegalArgumentException("There are no assets with the original asset ID '" + asset.getOriginalAssetId() + "'!");
            }
        }

        String message = "Asset '" + asset.getName() + "' is updated by '" + user.getUsername() + "'";

        // Send notification for subscribers
        List<User> subscribers = getSubscribers(asset.getId());
        for(User subscriber: subscribers){

            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
            sendNotificationRequest.setFromUsername(user.getUsername());
            sendNotificationRequest.setToUsername(subscriber.getUsername());
            sendNotificationRequest.setMessage(message);
            notificationUtils.sendNotification(sendNotificationRequest, session);
        }

        // Send notification for asset owners
        List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), assetId, true);
        for(InternalShare internalShare: internalShares){
            if(internalShare.getNotifyOnEdit().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES)){
                SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                sendNotificationRequest.setFromUsername(user.getUsername());
                sendNotificationRequest.setToUsername(internalShare.getUsername());
                sendNotificationRequest.setMessage(message);
                notificationUtils.sendNotification(sendNotificationRequest, session);
            }
        }

        return getAsset(assetId);
    }

    @LogEntryExitExecutionTime
    public boolean subscribeToAsset(String assetId, User user){
        Asset asset = getAsset(assetId);

        if(!isSubscribed(user, asset)){
            List<Asset> nonDeletedAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
            if(!nonDeletedAssetsByOriginalAssetId.isEmpty()){
                nonDeletedAssetsByOriginalAssetId.forEach(a -> assetUserRepository.insertSubscriber(a.getId(), user.getId()));
            }

            return true;
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public boolean unsubscribeFromAsset(String assetId, User user) throws Exception {
        Asset asset = getAsset(assetId);

        if(isSubscribed(user, asset)){
            User importUser = authenticationUtils.getUser(asset.getImportedByUsername());
            assetUserRepository.deleteSubscriber(assetId, importUser.getId());

            return true;
        }

        return false;
    }

    public void unsubscribeUsersFromAsset(String assetId){
        assetUserRepository.deleteAllSubscribersByAssetId(assetId);
    }

    @LogEntryExitExecutionTime
    public void createAsset(Asset asset){
        _logger.debug("Asset:");
        _logger.debug("Asset ID: " + asset.getId());
        _logger.debug("Asset Extension: " + asset.getExtension());
        _logger.debug("Asset Imported By Username: " + asset.getImportedByUsername());
        _logger.debug("Asset Name: " + asset.getName());
        _logger.debug("Asset Path: " + asset.getPath());
        _logger.debug("Asset Preview Path: " + asset.getPreviewPath());
        _logger.debug("Asset Thumbnail Path: " + asset.getThumbnailPath());
        _logger.debug("Asset Type: " + asset.getType());
        _logger.debug("Asset Import Date: " + asset.getImportDate());
        _logger.debug("Deleted: " + asset.getDeleted());
        _logger.debug("Checksum: " + asset.getChecksum());
        _logger.debug("Is latest version: " + asset.getIsLatestVersion());
        _logger.debug("Original Asset ID: " + asset.getOriginalAssetId());
        _logger.debug("Version: " + asset.getVersion());
        _logger.debug("File Size: " + asset.getFileSize());

        _logger.debug("Inserting asset into the database...");

        assetsRepository.insertAsset(asset.getId(), asset.getExtension(), asset.getImportedByUsername(), asset.getName(), asset.getPath(),
                asset.getPreviewPath(), asset.getThumbnailPath(), asset.getType(), asset.getImportDate(), asset.getDeleted(), asset.getChecksum(),
                asset.getIsLatestVersion(), asset.getOriginalAssetId(), asset.getVersion(), asset.getFileSize());
    }

    @LogEntryExitExecutionTime
    public List<User> getSubscribers(String assetId) {
        List<User> subscribers = new ArrayList<>();

        List<AssetUser> assetUsersByAssetId = assetUserRepository.findAssetUsersByAssetId(assetId);
        for(AssetUser assetUser: assetUsersByAssetId){
            User user = authenticationUtils.getUser(assetUser.getUserId());
            subscribers.add(user);
        }

        return subscribers;
    }

    @LogEntryExitExecutionTime
    public String generateAssetId(){
        String assetId = RandomStringUtils.randomAlphanumeric(ToyboxConstants.ASSET_ID_LENGTH);
        if(isAssetIdValid(assetId)){
            return assetId;
        }
        return generateAssetId();
    }

    @LogEntryExitExecutionTime
    private boolean isAssetIdValid(String assetId){
        List<Container> containers = containersRepository.getContainersById(assetId);
        List<Asset> assets = assetsRepository.getAssetsById(assetId);
        if(containers.isEmpty() && assets.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }
}
