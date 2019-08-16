package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.dbo.ContainerAsset;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.utilities.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class CommonObjectController {
    private static final Log _logger = LogFactory.getLog(CommonObjectController.class);

    private static final String jobServiceLoadBalancerServiceName = "toybox-job-loadbalancer";
    private static final String assetServiceLoadBalancerServiceName = "toybox-asset-loadbalancer";
    private static final String notificationServiceLoadBalancerServiceName = "toybox-notification-loadbalancer";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private AssetUserRepository assetUserRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ContainersRepository containersRepository;
    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;
    @Autowired
    private ContainerUsersRepository containerUsersRepository;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadObjects(Authentication authentication, HttpSession session, @RequestBody SelectionContext selectionContext){
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);

                    HttpEntity<SelectionContext> selectionContextHttpEntity = new HttpEntity<>(selectionContext, headers);

                    String jobServiceUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, jobServiceLoadBalancerServiceName);

                    ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/package", selectionContextHttpEntity, JobResponse.class);
                    if(jobResponseResponseEntity != null){
                        _logger.debug(jobResponseResponseEntity);
                        JobResponse jobResponse = jobResponseResponseEntity.getBody();
                        if(jobResponse != null){
                            _logger.debug("Job response message: " + jobResponse.getMessage());
                            _logger.debug("Job ID: " + jobResponse.getJobId());
                            File archiveFile = JobUtils.getInstance().getArchiveFile(jobResponse.getJobId(), headers, jobServiceUrl, exportStagingPath);
                            if(archiveFile != null && archiveFile.exists()){
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
                else{
                    String errorMessage = "Selection context is not valid!";
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
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                    if(user != null){
                        if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                            // We are adding the selected assets and the assets which are not deleted, set at their last version and in the selected containers to a list
                            List<Asset> selectedAssetsAndContainerAssets = new ArrayList<>(selectionContext.getSelectedAssets());

                            for(Container selectedContainer: selectionContext.getSelectedContainers()){
                                List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(selectedContainer.getId());
                                if(!containerAssetsByContainerId.isEmpty()){
                                    List<String> containerAssetIds = containerAssetsByContainerId.stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());
                                    List<Asset> assetsByAssetIds = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(containerAssetIds);
                                    selectedAssetsAndContainerAssets.addAll(assetsByAssetIds);
                                }
                            }

                            // We create another list for the final asset list
                            List<Asset> assetsAndVersions = new ArrayList<>(selectedAssetsAndContainerAssets);
                            // We find all the non-deleted versions of the assets and add them to a list
                            for(Asset selectedAsset: selectedAssetsAndContainerAssets){
                                List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                assetsAndVersions.addAll(assetsByOriginalAssetId);
                            }

                            if(!assetsAndVersions.isEmpty()){
                                // We set all the assets as deleted
                                assetsRepository.deleteAssetById("Y",assetsAndVersions.stream().map(Asset::getId).collect(Collectors.toList()));
                                // We send delete notification for the selected assets
                                for(Asset asset: selectedAssetsAndContainerAssets){
                                    // Send notification
                                    String message = "Asset '" + asset.getName() + "' is deleted by '" + user.getUsername() + "'";
                                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                    sendNotificationRequest.setAsset(asset);
                                    sendNotificationRequest.setFromUser(user);
                                    sendNotificationRequest.setMessage(message);
                                    NotificationUtils.getInstance().sendNotification(sendNotificationRequest, discoveryClient, session, notificationServiceLoadBalancerServiceName);
                                }
                                // We un-subscribe users from the deleted assets
                                for(Asset asset: assetsAndVersions){
                                    Asset actualAsset = AssetUtils.getInstance().getAsset(assetsRepository, asset.getId());
                                    User importUser = AuthenticationUtils.getInstance().getUser(usersRepository, actualAsset.getImportedByUsername());
                                    assetUserRepository.deleteSubscriber(actualAsset.getId(), importUser.getId());
                                }
                            }

                            if(!selectionContext.getSelectedContainers().isEmpty()){
                                // We set all the containers as deleted
                                containersRepository.deleteContainersById("Y", selectionContext.getSelectedContainers().stream().map(Container::getId).collect(Collectors.toList()));
                                // We send delete notification for the selected containers
                                // TODO: Send notification for folders
                                // We un-subscribe users from the deleted containers
                                for(Container selectedContainer: selectionContext.getSelectedContainers()){
                                    Container actualContainer = ContainerUtils.getInstance().getContainer(containersRepository, selectedContainer.getId());
                                    User createUser = AuthenticationUtils.getInstance().getUser(usersRepository, actualContainer.getCreatedByUsername());
                                    containerUsersRepository.deleteSubscriber(actualContainer.getId(), createUser.getId());
                                }
                            }

                            int numberOfDeletedAssets = selectedAssetsAndContainerAssets.size();
                            int numberOfDeletedContainers = selectionContext.getSelectedContainers().size();

                            genericResponse.setMessage(generateProcessingResponse(numberOfDeletedAssets, numberOfDeletedContainers, " deleted successfully."));

                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            String warningMessage = "No assets or folders are selected!";
                            _logger.warn(warningMessage);

                            genericResponse.setMessage(warningMessage);

                            return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
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
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    if(!(selectionContext.getSelectedAssets().isEmpty() && selectionContext.getSelectedContainers().isEmpty())){
                        User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                        if(user != null){
                            int assetCount = 0;
                            int containerCount = 0;
                            for(Asset selectedAsset: selectionContext.getSelectedAssets()){
                                if(!AssetUtils.getInstance().isSubscribed(assetUserRepository, user, selectedAsset)){
                                    List<Asset> nonDeletedAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                    if(!nonDeletedAssetsByOriginalAssetId.isEmpty()){
                                        nonDeletedAssetsByOriginalAssetId.forEach(asset -> assetUserRepository.insertSubscriber(asset.getId(), user.getId()));
                                        assetCount++;
                                    }
                                }
                            }
                            for(Container selectedContainer: selectionContext.getSelectedContainers()){
                                if(!ContainerUtils.getInstance().isSubscribed(containerUsersRepository, user, selectedContainer)){
                                    containerUsersRepository.insertSubscriber(selectedContainer.getId(), user.getId());

                                    List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(selectedContainer.getId());
                                    for(ContainerAsset containerAsset: containerAssetsByContainerId){
                                        Asset actualAsset = AssetUtils.getInstance().getAsset(assetsRepository, containerAsset.getAssetId());
                                        if(!AssetUtils.getInstance().isSubscribed(assetUserRepository, user, actualAsset)){
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

                            if(assetCount > 0 || containerCount > 0){
                                genericResponse.setMessage(generateProcessingResponse(assetCount, containerCount, " subscribed successfully."));
                            }
                            else{
                                genericResponse.setMessage("Selected assets were already subscribed.");
                            }

                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            throw new IllegalArgumentException("User is null");
                        }
                    }
                    else{
                        String warningMessage = "No assets or folders are selected!";
                        _logger.warn(warningMessage);

                        genericResponse.setMessage(warningMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
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
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null && SelectionUtils.getInstance().isSelectionContextValid(selectionContext)){
                    User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                    if(user != null){
                        int assetCount = 0;
                        int containerCount = 0;

                        for(Asset selectedAsset: selectionContext.getSelectedAssets()){
                            if(AssetUtils.getInstance().isSubscribed(assetUserRepository, user, selectedAsset)){
                                List<Asset> nonDeletedAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                if(!nonDeletedAssetsByOriginalAssetId.isEmpty()){
                                    nonDeletedAssetsByOriginalAssetId.forEach(asset -> assetUserRepository.deleteSubscriber(asset.getId(), user.getId()));
                                    assetCount++;
                                }
                            }
                        }

                        for(Container selectedContainer: selectionContext.getSelectedContainers()){
                            if(ContainerUtils.getInstance().isSubscribed(containerUsersRepository, user, selectedContainer)){
                                containerUsersRepository.deleteSubscriber(selectedContainer.getId(), user.getId());

                                List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(selectedContainer.getId());
                                for(ContainerAsset containerAsset: containerAssetsByContainerId){
                                    Asset actualAsset = AssetUtils.getInstance().getAsset(assetsRepository, containerAsset.getAssetId());
                                    if(AssetUtils.getInstance().isSubscribed(assetUserRepository, user, actualAsset)){
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

                        if(assetCount > 0 || containerCount > 0){
                            genericResponse.setMessage(generateProcessingResponse(assetCount, containerCount, " unsubscribed successfully."));
                        }
                        else{
                            genericResponse.setMessage("Selected assets were already unsubscribed.");
                        }

                        return new ResponseEntity<>(genericResponse, HttpStatus.OK);
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
            String errorMessage = "An error occurred while unsubscribing from assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    private String generateProcessingResponse(int assetCount, int containerCount, String endOfMessage){
        boolean hasProcessedAssets = assetCount > 0;
        boolean hasProcessedContainers = containerCount > 0;
        String assetSuffix = assetCount > 1 ? "s" : "";
        String containerSuffix = containerCount > 1 ? "s" :"";
        String assetMessage = hasProcessedAssets ? (assetCount + " asset" + assetSuffix) : "";
        String containerMessage = hasProcessedContainers ? (containerCount + " folder" + containerSuffix) : "";

        String message = "";
        if(hasProcessedAssets && hasProcessedContainers){
            message = assetMessage + " and " + containerMessage;
        }
        else if(hasProcessedAssets){
            message = assetMessage;
        }
        else if(hasProcessedContainers){
            message = containerMessage;
        }
        else{
            throw new IllegalArgumentException("Unexpected input received while generating the processing response! [Asset count: " + assetCount + "][Container count: " + containerCount + "]");
        }

        message += endOfMessage;

        return message;
    }
}