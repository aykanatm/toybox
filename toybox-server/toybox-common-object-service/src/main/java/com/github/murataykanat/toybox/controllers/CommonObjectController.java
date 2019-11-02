package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.asset.CopyAssetRequest;
import com.github.murataykanat.toybox.schema.asset.MoveAssetRequest;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.utilities.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class CommonObjectController {
    private static final Log _logger = LogFactory.getLog(CommonObjectController.class);

    @Autowired
    private AssetUtils assetUtils;
    @Autowired
    private ContainerUtils containerUtils;
    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private NotificationUtils notificationUtils;
    @Autowired
    private SelectionUtils selectionUtils;
    @Autowired
    private JobUtils jobUtils;
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
    @Autowired
    private ContainerUsersRepository containerUsersRepository;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @Value("${importStagingPath}")
    private String importStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadObjects(Authentication authentication, HttpSession session, @RequestBody SelectionContext selectionContext){
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    if(selectionContext != null && selectionUtils.isSelectionContextValid(selectionContext)){
                        if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                            RestTemplate restTemplate = new RestTemplate();
                            HttpHeaders headers = authenticationUtils.getHeaders(session);

                            HttpEntity<SelectionContext> selectionContextHttpEntity = new HttpEntity<>(selectionContext, headers);

                            String jobServiceUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.JOB_SERVICE_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.JOB_SERVICE_NAME, session, false);

                            try{
                                ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/package", selectionContextHttpEntity, JobResponse.class);
                                if(jobResponseResponseEntity != null){
                                    JobResponse jobResponse = jobResponseResponseEntity.getBody();
                                    if(jobResponse != null){
                                        File archiveFile = jobUtils.getArchiveFile(jobResponse.getJobId(), headers, jobServiceUrl, exportStagingPath);
                                        if(archiveFile != null && archiveFile.exists()){

                                            List<Asset> selectedAssets = selectionContext.getSelectedAssets();
                                            List<Container> selectedContainers = selectionContext.getSelectedContainers();

                                            for(Asset selectedAsset: selectedAssets){
                                                // Send notification for asset owners
                                                List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), selectedAsset.getId(), true);
                                                for(InternalShare internalShare: internalShares){
                                                    if(internalShare.getNotifyOnDownload().equalsIgnoreCase("Y")){
                                                        String message = "Asset '" + selectedAsset.getName() + "' is downloaded by '" + user.getUsername() + "'";
                                                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                                        sendNotificationRequest.setFromUsername(user.getUsername());
                                                        sendNotificationRequest.setToUsername(internalShare.getUsername());
                                                        sendNotificationRequest.setMessage(message);
                                                        notificationUtils.sendNotification(sendNotificationRequest, session);
                                                    }
                                                }
                                            }

                                            for(Container selectedContainer: selectedContainers){
                                                // Send notification for container owners
                                                List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), selectedContainer.getId(), false);
                                                for(InternalShare internalShare: internalShares){
                                                    if(internalShare.getNotifyOnDownload().equalsIgnoreCase("Y")){
                                                        String message = "Folder '" + selectedContainer.getName() + "' is downloaded by '" + user.getUsername() + "'";
                                                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                                        sendNotificationRequest.setFromUsername(user.getUsername());
                                                        sendNotificationRequest.setToUsername(internalShare.getUsername());
                                                        sendNotificationRequest.setMessage(message);
                                                        notificationUtils.sendNotification(sendNotificationRequest, session);
                                                    }
                                                }
                                            }


                                            InputStreamResource resource = new InputStreamResource(new FileInputStream(archiveFile));
                                            return new ResponseEntity<>(resource, HttpStatus.OK);
                                        }
                                        else{
                                            if(archiveFile != null){
                                                throw new IOException("File '" + archiveFile.getAbsolutePath() + "' does not exist!");
                                            }
                                            throw new IllegalArgumentException("Archive file is null!");
                                        }
                                    }
                                    else{
                                        throw new IllegalArgumentException("Job response is null!");
                                    }
                                }
                                else{
                                    throw new IllegalArgumentException("Job response entity is null!");
                                }
                            }
                            catch (HttpStatusCodeException httpEx){
                                JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
                                throw new Exception("Upload was successful but import failed to start. " + responseJson.get("message").getAsString());
                            }
                        }
                        else{
                            String errorMessage = "No assets or folders are selected!";
                            _logger.error(errorMessage);

                            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                        }
                    }
                    else{
                        String errorMessage = "Selection context is not valid!";
                        _logger.error(errorMessage);

                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
            String errorMessage = "An error occurred while downloading selected assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> deleteObjects(Authentication authentication, HttpSession session, @RequestBody SelectionContext selectionContext){
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(selectionContext != null && selectionUtils.isSelectionContextValid(selectionContext)){
                    User user = authenticationUtils.getUser(authentication);
                    if(user != null){
                        if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                            boolean hasSharedAssets = selectionContext.getSelectedAssets().stream().anyMatch(asset -> asset.getShared().equalsIgnoreCase("Y"));
                            boolean hasSharedContainers = selectionContext.getSelectedContainers().stream().anyMatch(container -> container.getShared().equalsIgnoreCase("Y"));
                            if(!hasSharedAssets && !hasSharedContainers){
                                // We filter out the selected shared assets
                                List<Asset> nonSharedSelectedAssets = selectionContext.getSelectedAssets().stream().filter(asset -> !shareUtils.isAssetSharedWithUser(user.getId(), asset.getId())).collect(Collectors.toList());
                                List<Container> nonSharedSelectedContainers = selectionContext.getSelectedContainers().stream().filter(container -> !shareUtils.isContainerSharedWithUser(user.getId(), container.getId())).collect(Collectors.toList());

                                // We are adding a refreshed list of assets to the list of assets which will be deleted
                                List<Asset> selectedAssetsAndContainerAssets = new ArrayList<>();
                                if(!nonSharedSelectedAssets.isEmpty()){
                                    selectedAssetsAndContainerAssets.addAll(assetsRepository.getAssetsByAssetIds(nonSharedSelectedAssets.stream().map(Asset::getId).collect(Collectors.toList())));
                                }

                                // We are adding the non-shared last version of the assets inside the containers that was selected as deleted to the list of assets which will be deleted.
                                for(Container selectedContainer: nonSharedSelectedContainers){
                                    List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(selectedContainer.getId());
                                    if(!containerAssetsByContainerId.isEmpty()){
                                        List<String> containerAssetIds = containerAssetsByContainerId.stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());
                                        List<String> nonSharedContainerAssetIds = containerAssetIds.stream().filter(assetId -> !shareUtils.isAssetSharedWithUser(user.getId(), assetId)).collect(Collectors.toList());
                                        List<Asset> assetsByAssetIds = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(nonSharedContainerAssetIds);
                                        selectedAssetsAndContainerAssets.addAll(assetsByAssetIds);
                                    }
                                }

                                // We create another list for the final asset list
                                List<Asset> assetsAndVersions = new ArrayList<>(selectedAssetsAndContainerAssets);
                                // We find all the non-deleted versions of the assets if the selected asset is the latest version and add them to a list
                                for(Asset selectedAsset: selectedAssetsAndContainerAssets){
                                    if(selectedAsset.getIsLatestVersion().equalsIgnoreCase("Y")){
                                        List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                        assetsAndVersions.addAll(assetsByOriginalAssetId);
                                    }
                                }

                                if(!assetsAndVersions.isEmpty()){
                                    // We set all the assets as deleted
                                    assetsRepository.deleteAssetById("Y",assetsAndVersions.stream().map(Asset::getId).collect(Collectors.toList()));
                                    // We send delete notification for the selected assets
                                    for(Asset asset: selectedAssetsAndContainerAssets){
                                        // Send notification for subscribers
                                        List<User> subscribers = assetUtils.getSubscribers(asset.getId());
                                        for(User subscriber: subscribers){
                                            String message = "Asset '" + asset.getName() + "' is deleted by '" + user.getUsername() + "'";
                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                            sendNotificationRequest.setFromUsername(user.getUsername());
                                            sendNotificationRequest.setToUsername(subscriber.getUsername());
                                            sendNotificationRequest.setMessage(message);
                                            notificationUtils.sendNotification(sendNotificationRequest, session);
                                        }
                                    }
                                    // We un-subscribe users from the deleted assets
                                    for(Asset asset: assetsAndVersions){
                                        Asset actualAsset = assetUtils.getAsset(asset.getId());
                                        User importUser = authenticationUtils.getUser(actualAsset.getImportedByUsername());
                                        assetUserRepository.deleteSubscriber(actualAsset.getId(), importUser.getId());
                                    }
                                    // We remove the assets from shares
                                    for(Asset asset: assetsAndVersions){
                                        List<InternalShare> internalSharesContainingItem = shareUtils.getInternalSharesContainingItem(asset.getId(), true);
                                        for(InternalShare internalShare: internalSharesContainingItem){
                                            shareUtils.removeItemFromInternalShare(internalShare.getInternalShareId(), asset.getId(), true);
                                        }
                                    }
                                }

                                if(!nonSharedSelectedContainers.isEmpty()){
                                    // We set all the non-shared containers as deleted
                                    containersRepository.deleteContainersById("Y", nonSharedSelectedContainers.stream().map(Container::getId).collect(Collectors.toList()));
                                    // We send delete notification for the selected containers
                                    for(Container selectedContainer: nonSharedSelectedContainers){
                                        // Send notification for subscribers
                                        List<User> subscribers = containerUtils.getSubscribers(selectedContainer.getId());
                                        for(User subscriber: subscribers){
                                            String message = "Folder '" + selectedContainer.getName() + "' is deleted by '" + user.getUsername() + "'";
                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                            sendNotificationRequest.setFromUsername(user.getUsername());
                                            sendNotificationRequest.setToUsername(subscriber.getUsername());
                                            sendNotificationRequest.setMessage(message);
                                            notificationUtils.sendNotification(sendNotificationRequest, session);
                                        }
                                    }
                                    // We un-subscribe users from the deleted containers
                                    for(Container selectedContainer: nonSharedSelectedContainers){
                                        Container actualContainer = containerUtils.getContainer(selectedContainer.getId());
                                        User createUser = authenticationUtils.getUser(actualContainer.getCreatedByUsername());
                                        containerUsersRepository.deleteSubscriber(actualContainer.getId(), createUser.getId());
                                    }
                                    // We remove the containers and their assets from shares
                                    for(Container selectedContainer: nonSharedSelectedContainers){
                                        List<InternalShare> internalSharesContainingItem = shareUtils.getInternalSharesContainingItem(selectedContainer.getId(), false);
                                        for(InternalShare internalShare: internalSharesContainingItem){
                                            shareUtils.removeItemFromInternalShare(internalShare.getInternalShareId(), selectedContainer.getId(), true);
                                        }
                                    }
                                }

                                int numberOfDeletedAssets = selectedAssetsAndContainerAssets.size();
                                int numberOfDeletedContainers = nonSharedSelectedContainers.size();

                                String failureMessage = "You do not have the permission to delete the selected assets and/or folders.";
                                String message = generateProcessingResponse(numberOfDeletedAssets, numberOfDeletedContainers, " deleted successfully.", failureMessage);

                                genericResponse.setMessage(message);

                                if(message.equalsIgnoreCase(failureMessage)){
                                    return new ResponseEntity<>(genericResponse, HttpStatus.FORBIDDEN);
                                }
                                else{
                                    return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                }
                            }
                            else{
                                String errorMessage = "You do not have the permission to delete one of the selected items!";
                                _logger.error(errorMessage);

                                genericResponse.setMessage(errorMessage);

                                return new ResponseEntity<>(genericResponse, HttpStatus.FORBIDDEN);
                            }
                        }
                        else{
                            String errorMessage = "No assets or folders are selected!";
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
                    String errorMessage = "Selection context is not valid!";
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
            String errorMessage = "An error occurred while deleting assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/subscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> subscribeToObjects(Authentication authentication, @RequestBody SelectionContext selectionContext){
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(selectionContext != null && selectionUtils.isSelectionContextValid(selectionContext)){
                    if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                        User user = authenticationUtils.getUser(authentication);
                        if(user != null){
                            int assetCount = 0;
                            int containerCount = 0;
                            for(Asset selectedAsset: selectionContext.getSelectedAssets()){
                                if(!assetUtils.isSubscribed(assetUserRepository, user, selectedAsset)){
                                    List<Asset> nonDeletedAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                    if(!nonDeletedAssetsByOriginalAssetId.isEmpty()){
                                        nonDeletedAssetsByOriginalAssetId.forEach(asset -> assetUserRepository.insertSubscriber(asset.getId(), user.getId()));
                                        assetCount++;
                                    }
                                }
                            }
                            for(Container selectedContainer: selectionContext.getSelectedContainers()){
                                if(!containerUtils.isSubscribed(user, selectedContainer)){
                                    containerUsersRepository.insertSubscriber(selectedContainer.getId(), user.getId());

                                    List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(selectedContainer.getId());
                                    for(ContainerAsset containerAsset: containerAssetsByContainerId){
                                        Asset actualAsset = assetUtils.getAsset(containerAsset.getAssetId());
                                        if(!assetUtils.isSubscribed(assetUserRepository, user, actualAsset)){
                                            List<Asset> nonDeletedAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(actualAsset.getOriginalAssetId());
                                            if(!nonDeletedAssetsByOriginalAssetId.isEmpty()){
                                                nonDeletedAssetsByOriginalAssetId.forEach(asset -> assetUserRepository.insertSubscriber(asset.getId(), user.getId()));
                                                assetCount++;
                                            }
                                        }
                                    }
                                    containerCount++;
                                }
                            }

                            String failureMessage = "Selected assets were already subscribed.";
                            String message = generateProcessingResponse(assetCount, containerCount, " unsubscribed successfully.", failureMessage);

                            genericResponse.setMessage(message);

                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            throw new IllegalArgumentException("User is null");
                        }
                    }
                    else{
                        String errorMessage = "No assets or folders are selected!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Selection context is not valid!";
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
            String errorMessage = "An error occurred while subscribing to assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/unsubscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> unsubscribeFromObjects(Authentication authentication, @RequestBody SelectionContext selectionContext){
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(selectionContext != null && selectionUtils.isSelectionContextValid(selectionContext)){
                    if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                        User user = authenticationUtils.getUser(authentication);
                        if(user != null){
                            int assetCount = 0;
                            int containerCount = 0;

                            for(Asset selectedAsset: selectionContext.getSelectedAssets()){
                                if(assetUtils.isSubscribed(assetUserRepository, user, selectedAsset)){
                                    List<Asset> nonDeletedAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                    if(!nonDeletedAssetsByOriginalAssetId.isEmpty()){
                                        nonDeletedAssetsByOriginalAssetId.forEach(asset -> assetUserRepository.deleteSubscriber(asset.getId(), user.getId()));
                                        assetCount++;
                                    }
                                }
                            }

                            for(Container selectedContainer: selectionContext.getSelectedContainers()){
                                if(containerUtils.isSubscribed(user, selectedContainer)){
                                    containerUsersRepository.deleteSubscriber(selectedContainer.getId(), user.getId());

                                    List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(selectedContainer.getId());
                                    for(ContainerAsset containerAsset: containerAssetsByContainerId){
                                        Asset actualAsset = assetUtils.getAsset(containerAsset.getAssetId());
                                        if(assetUtils.isSubscribed(assetUserRepository, user, actualAsset)){
                                            List<Asset> nonDeletedAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(actualAsset.getOriginalAssetId());
                                            if(!nonDeletedAssetsByOriginalAssetId.isEmpty()){
                                                nonDeletedAssetsByOriginalAssetId.forEach(asset -> assetUserRepository.deleteSubscriber(asset.getId(), user.getId()));
                                                assetCount++;
                                            }
                                        }
                                    }
                                    containerCount++;
                                }
                            }

                            String failureMessage = "Selected assets were already unsubscribed.";
                            String message = generateProcessingResponse(assetCount, containerCount, " unsubscribed successfully.", failureMessage);

                            genericResponse.setMessage(message);

                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            throw new IllegalArgumentException("User is null!");
                        }
                    }
                    else{
                        String errorMessage = "No assets or folders are selected!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Selection context is not valid!";
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
            String errorMessage = "An error occurred while unsubscribing from assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/move", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> moveItems(Authentication authentication, HttpSession session, @RequestBody MoveAssetRequest moveAssetRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(moveAssetRequest != null){
                    User user = authenticationUtils.getUser(authentication.getName());
                    if(user != null){
                        SelectionContext selectionContext = moveAssetRequest.getSelectionContext();
                        if(selectionContext != null && selectionUtils.isSelectionContextValid(selectionContext)){
                            if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                                boolean hasSharedAssets = selectionContext.getSelectedAssets().stream().anyMatch(asset -> asset.getShared().equalsIgnoreCase("Y"));
                                boolean hasSharedContainers = selectionContext.getSelectedContainers().stream().anyMatch(container -> container.getShared().equalsIgnoreCase("Y"));
                                if(!hasSharedAssets && !hasSharedContainers){
                                    Container targetContainer = containerUtils.getContainer(moveAssetRequest.getContainerId());

                                    List<String> assetIds = selectionContext.getSelectedAssets().stream().map(Asset::getId).collect(Collectors.toList());
                                    List<String> containerIds = selectionContext.getSelectedContainers().stream().map(Container::getId).collect(Collectors.toList());

                                    int assetCount = 0;
                                    int containerCount = 0;

                                    int numberOfIgnoredAssets = assetUtils.moveAssets(assetIds, targetContainer, user, session);
                                    assetCount += (assetIds.size() - numberOfIgnoredAssets);
                                    int numberOfIgnoredContainers = containerUtils.moveContainers(containerIds, targetContainer, user, session);
                                    containerCount += (containerIds.size() - numberOfIgnoredContainers);

                                    if(assetCount == 0 && containerCount == 0){
                                        genericResponse.setMessage("No asset or folder is moved because either the target folder is the same as the selected folders or the target folder is a sub folder of the selected folders.");
                                        return new ResponseEntity<>(genericResponse, HttpStatus.NOT_MODIFIED);
                                    }
                                    else if(assetCount < 0 || containerCount < 0){
                                        throw new IllegalArgumentException("Returned asset or container is below zero!");
                                    }
                                    else{
                                        String failureMessage = "You do not have the permission to move the selected assets and/or folders.";
                                        String message = generateProcessingResponse(assetCount, containerCount, " were successfully moved to the folder '" + targetContainer.getName() + "'.", failureMessage);

                                        genericResponse.setMessage(message);

                                        if(message.equalsIgnoreCase(failureMessage)){
                                            return new ResponseEntity<>(genericResponse, HttpStatus.FORBIDDEN);
                                        }
                                        else{
                                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                        }
                                    }
                                }
                                else{
                                    String errorMessage = "You do not have the permission to move one of the selected items!";
                                    _logger.error(errorMessage);

                                    genericResponse.setMessage(errorMessage);

                                    return new ResponseEntity<>(genericResponse, HttpStatus.FORBIDDEN);
                                }
                            }
                            else{
                                String errorMessage = "No assets or folders are selected!";
                                _logger.error(errorMessage);

                                genericResponse.setMessage(errorMessage);

                                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                            }
                        }
                        else{
                            String errorMessage = "Selection context is not valid!";
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
                    String errorMessage = "Asset move request is null!";
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
            String errorMessage = "An error occurred while moving assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/copy", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> copyItems(Authentication authentication, HttpSession session, @RequestBody CopyAssetRequest copyAssetRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(copyAssetRequest != null){
                    SelectionContext selectionContext = copyAssetRequest.getSelectionContext();
                    if(selectionContext != null && selectionUtils.isSelectionContextValid(selectionContext)){
                        if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                            List<String> targetContainerIds = copyAssetRequest.getContainerIds();
                            List<String> sourceAssetIds = selectionContext.getSelectedAssets().stream().map(Asset::getId).collect(Collectors.toList());
                            List<String> sourceContainerIds = selectionContext.getSelectedContainers().stream().map(Container::getId).collect(Collectors.toList());

                            int assetCount = 0;
                            int containerCount = 0;

                            assetUtils.copyAssets(session, sourceAssetIds, targetContainerIds, authentication.getName(), importStagingPath);
                            assetCount += sourceAssetIds.size();
                            containerUtils.copyContainers(session, sourceContainerIds, targetContainerIds, authentication.getName(), importStagingPath);
                            containerCount += sourceContainerIds.size();

                            String failureMessage = "You do not have the permission to copy the selected assets and/or folders.";
                            String message = generateProcessingResponse(assetCount, containerCount, " started to be copied to the selected folders. This action may take a while depending on the number and size of the selected assets and folders. You can follow the progress of the copy operations in 'Jobs' section.", failureMessage);

                            genericResponse.setMessage(message);

                            if(message.equalsIgnoreCase(failureMessage)){
                                return new ResponseEntity<>(genericResponse, HttpStatus.FORBIDDEN);
                            }
                            else{
                                return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                            }
                        }
                        else{
                            String errorMessage = "No assets or folders are selected!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                        }
                    }
                    else{
                        String errorMessage = "Selection context is not valid!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Copy move request is null!";
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
            String errorMessage = "An error occurred while copying assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    private String generateProcessingResponse(int assetCount, int containerCount, String endOfMessage, String failureMessage){
        boolean hasProcessedAssets = assetCount > 0;
        boolean hasProcessedContainers = containerCount > 0;

        String assetSuffix = assetCount > 1 ? "s" : "";
        String containerSuffix = containerCount > 1 ? "s" :"";
        String assetMessage = hasProcessedAssets ? (assetCount + " asset" + assetSuffix) : "";
        String containerMessage = hasProcessedContainers ? (containerCount + " folder" + containerSuffix) : "";

        String message = "";
        if(hasProcessedAssets && hasProcessedContainers){
            message = assetMessage + " and " + containerMessage + endOfMessage;
        }
        else if(hasProcessedAssets){
            message = assetMessage + endOfMessage;
        }
        else if(hasProcessedContainers){
            message = containerMessage + endOfMessage;
        }
        else if(!hasProcessedAssets && !hasProcessedContainers){
            message = failureMessage;
        }
        else{
            throw new IllegalArgumentException("Unexpected input received while generating the processing response! [Asset count: " + assetCount + "][Container count: " + containerCount + "]");
        }

        return message;
    }
}