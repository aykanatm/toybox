package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.*;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class AssetController {
    private static final Log _logger = LogFactory.getLog(AssetController.class);

    private static final String commonObjectsLoadBalancerServiceName = "toybox-common-object-loadbalancer";
    private static final String jobServiceLoadBalancerServiceName = "toybox-job-loadbalancer";
    private static final String notificationsLoadBalancerServiceName = "toybox-notification-loadbalancer";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private AssetUserRepository assetUserRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @Value("${importStagingPath}")
    private String importStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/assets/upload", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> uploadAssets(Authentication authentication, HttpSession session, @RequestBody UploadFileLst uploadFileLst) {
        try{
            GenericResponse genericResponse = new GenericResponse();
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(uploadFileLst != null){
                    RestTemplate restTemplate = new RestTemplate();

                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    HttpEntity<UploadFileLst> selectedAssetsEntity = new HttpEntity<>(uploadFileLst, headers);
                    String jobServiceUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, jobServiceLoadBalancerServiceName);
                    ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/import", selectedAssetsEntity, JobResponse.class);
                    boolean successful = jobResponseResponseEntity.getStatusCode().is2xxSuccessful();

                    if(successful){
                        genericResponse.setMessage(uploadFileLst.getUploadFiles().size() + " file(s) successfully uploaded. Import job started.");
                        return new ResponseEntity<>(genericResponse, HttpStatus.CREATED);
                    }
                    else{
                        genericResponse.setMessage("Upload was successful but import failed to start. " + jobResponseResponseEntity.getBody().getMessage());
                        return new ResponseEntity<>(genericResponse, jobResponseResponseEntity.getStatusCode());
                    }
                }
                else{
                    String errorMessage = "Upload file list parameter is null!";
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
            String errorMessage = "An error occurred while starting the import job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/assets/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveAssetsResults> retrieveAssets(Authentication authentication, @RequestBody AssetSearchRequest assetSearchRequest){
        try{
            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(assetSearchRequest != null){
                    User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                    if(user != null){
                        String sortColumn = assetSearchRequest.getSortColumn();
                        String sortType = assetSearchRequest.getSortType();
                        int offset = assetSearchRequest.getOffset();
                        int limit = assetSearchRequest.getLimit();
                        List<SearchRequestFacet> searchRequestFacetList = assetSearchRequest.getAssetSearchRequestFacetList();

                        List<Asset> allAssets = assetsRepository.getNonDeletedAssets();
                        if(!allAssets.isEmpty()){
                            List<Asset> assets;

                            if(searchRequestFacetList != null && !searchRequestFacetList.isEmpty()){
                                assets = allAssets.stream().filter(asset -> FacetUtils.getInstance().hasFacetValue(asset, searchRequestFacetList)).collect(Collectors.toList());
                            }
                            else{
                                assets = allAssets;
                            }

                            List<Asset> assetsByCurrentUser = new ArrayList<>();
                            if(AuthenticationUtils.getInstance().isAdminUser(authentication)){
                                _logger.debug("Retrieving all assets [Admin User]...");
                                assetsByCurrentUser = assets.stream()
                                        .filter(asset -> asset.getIsLatestVersion().equalsIgnoreCase("Y"))
                                        .collect(Collectors.toList());
                            }
                            else{
                                _logger.debug("Retrieving assets of the user '" + user.getUsername() + "'...");

                                for(Asset asset: assets){
                                    if(StringUtils.isNotBlank(asset.getImportedByUsername()) && asset.getIsLatestVersion().equalsIgnoreCase("Y")){
                                        if(asset.getImportedByUsername().equalsIgnoreCase(user.getUsername())){
                                            assetsByCurrentUser.add(asset);
                                        }
                                        else{
                                            List<Asset> assetsById = assetsRepository.getAssetsById(asset.getOriginalAssetId());
                                            if(!assetsById.isEmpty()){
                                                if(assetsById.size() == 1){
                                                    Asset originalAsset = assetsById.get(0);
                                                    if(originalAsset.getImportedByUsername().equalsIgnoreCase(user.getUsername())){
                                                        assetsByCurrentUser.add(asset);
                                                    }
                                                }
                                                else{
                                                    throw new Exception("Asset with ID '" + asset.getId() + " has multiple original assets!");
                                                }
                                            }
                                            else{
                                                throw new Exception("Asset with ID '" + asset.getId() + " does not have a original asset!");
                                            }
                                        }
                                    }
                                }
                            }

                            // Set facets
                            List<Facet> facets = FacetUtils.getInstance().getFacets(assetsByCurrentUser);
                            retrieveAssetsResults.setFacets(facets);

                            // Sort assets
                            if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
                                SortUtils.getInstance().sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                                SortUtils.getInstance().sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            }

                            // Paginate assets
                            int totalRecords = assetsByCurrentUser.size();
                            int startIndex = offset;
                            int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                            List<Asset> assetsOnPage = assetsByCurrentUser.subList(startIndex, endIndex);

                            List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());

                            // Set subscription status
                            for(Asset assetOnPage: assetsOnPage){
                                assetOnPage.setSubscribed("N");

                                for(AssetUser assetUser: assetUsersByUserId){
                                    if(assetOnPage.getId().equalsIgnoreCase(assetUser.getAssetId())){
                                        assetOnPage.setSubscribed("Y");
                                        break;
                                    }
                                }
                            }

                            // Set parent container IDs
                            for(Asset assetOnPage: assetsOnPage){
                                List<ContainerAsset> containerAssetsByAssetId = containerAssetsRepository.findContainerAssetsByAssetId(assetOnPage.getId());
                                if(!containerAssetsByAssetId.isEmpty()){
                                    if(containerAssetsByAssetId.size() == 1){
                                        ContainerAsset containerAsset = containerAssetsByAssetId.get(0);
                                        assetOnPage.setParentContainerId(containerAsset.getContainerId());
                                    }
                                    else{
                                        throw new Exception("Asset is in multiple folders!");
                                    }
                                }
                                else{
                                    throw new Exception("Asset is not in any folder!");
                                }
                            }

                            retrieveAssetsResults.setTotalRecords(totalRecords);
                            retrieveAssetsResults.setAssets(assetsOnPage);

                            retrieveAssetsResults.setMessage("Assets retrieved successfully!");
                            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.OK);
                        }
                        else{
                            String message = "There are no assets to return.";
                            _logger.debug(message);

                            retrieveAssetsResults.setMessage(message);

                            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.NO_CONTENT);
                        }
                    }
                    else{
                        throw new IllegalArgumentException("User is null!");
                    }
                }
                else{
                    String errorMessage = "Asset search request is null!";
                    _logger.debug(errorMessage);

                    retrieveAssetsResults.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveAssetsResults.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/assets/{assetId}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateAsset(Authentication authentication, HttpSession session, @RequestBody UpdateAssetRequest updateAssetRequest, @PathVariable String assetId){
        GenericResponse genericResponse = new GenericResponse();

        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(updateAssetRequest != null){
                        User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                        if(user != null){
                            Asset oldAsset = AssetUtils.getInstance().getAsset(assetsRepository, assetId);
                            Asset asset = AssetUtils.getInstance().updateAsset(assetsRepository, updateAssetRequest, assetId);
                            if(asset != null){
                                // Send notification
                                String notification = "Asset '" + oldAsset.getName() + "' is updated by '" + user.getUsername() + "'";
                                SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                sendNotificationRequest.setIsAsset(true);
                                sendNotificationRequest.setId(oldAsset.getId());
                                sendNotificationRequest.setFromUser(user);
                                sendNotificationRequest.setMessage(notification);
                                NotificationUtils.getInstance().sendNotification(sendNotificationRequest, discoveryClient, session, notificationsLoadBalancerServiceName);

                                String message = "Asset updated successfully.";
                                _logger.debug(message);

                                genericResponse.setMessage(message);

                                return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                            }
                            else{
                                throw new IllegalArgumentException("Asset update failed!");
                            }
                        }
                        else{
                            throw new IllegalArgumentException("User is null!");
                        }
                    }
                    else{
                        String errorMessage = "Update asset request is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Asset ID is blank!";
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
            String errorMessage = "An error occurred while updating from assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/assets/{assetId}/versions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssetVersionResponse> getVersionHistory(Authentication authentication, @PathVariable String assetId){
        AssetVersionResponse assetVersionResponse = new AssetVersionResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                    if(user != null){
                        List<Asset> assetsById = assetsRepository.getAssetsById(assetId);
                        if(!assetsById.isEmpty()){
                            if(assetsById.size() == 1){
                                Asset asset = assetsById.get(0);

                                List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                                if(!assetsByOriginalAssetId.isEmpty()){
                                    SortUtils.getInstance().sortItems("des", assetsByOriginalAssetId, Comparator.comparing(Asset::getVersion));

                                    assetVersionResponse.setAssets(assetsByOriginalAssetId);
                                    assetVersionResponse.setMessage("Asset version history retrieved successfully.");

                                    return new ResponseEntity<>(assetVersionResponse, HttpStatus.OK);
                                }
                                else{
                                    throw new IllegalArgumentException("No assets found with original asset ID '" + asset.getOriginalAssetId() + "'!");
                                }
                            }
                            else{
                                throw new IllegalArgumentException("There are multiple assets with ID '" + assetId + "'!");
                            }
                        }
                        else{
                            String errorMessage = "No assets found with ID '" + assetId + "'.";
                            _logger.error(errorMessage);

                            assetVersionResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(assetVersionResponse, HttpStatus.NOT_FOUND);
                        }
                    }
                    else{
                        throw new IllegalArgumentException("User is null!");
                    }
                }
                else{
                    String errorMessage = "Asset ID is blank!";
                    _logger.error(errorMessage);

                    assetVersionResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(assetVersionResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                assetVersionResponse.setMessage(errorMessage);

                return new ResponseEntity<>(assetVersionResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving asset version history for asset with ID '" + assetId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            assetVersionResponse.setMessage(errorMessage);

            return new ResponseEntity<>(assetVersionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/assets/{assetId}/revert", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> revertAssetToVersion(Authentication authentication, HttpSession session, @PathVariable String assetId, @RequestBody RevertAssetVersionRequest revertAssetVersionRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(revertAssetVersionRequest != null){
                        User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                        if(user != null){
                            Asset asset = AssetUtils.getInstance().getAsset(assetsRepository, assetId);
                            List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                            if(!assetsByOriginalAssetId.isEmpty()){
                                SortUtils.getInstance().sortItems("des", assetsByOriginalAssetId, Comparator.comparing(Asset::getVersion));
                                List<Asset> assetsToDelete = new ArrayList<>();
                                for(Asset versionAsset: assetsByOriginalAssetId){
                                    if(versionAsset.getVersion() > revertAssetVersionRequest.getVersion()){
                                        assetsToDelete.add(versionAsset);
                                    }
                                    else if(versionAsset.getVersion() == revertAssetVersionRequest.getVersion()){
                                        List<String> assetIds = new ArrayList<>();
                                        assetIds.add(versionAsset.getId());

                                        assetsRepository.updateAssetsLatestVersion("Y", assetIds);
                                    }
                                }

                                if(!assetsToDelete.isEmpty()){
                                    assetsRepository.updateAssetsLatestVersion("N", assetsToDelete.stream().map(Asset::getId).collect(Collectors.toList()));
                                    List<Container> containersToDelete = new ArrayList<>();

                                    SelectionContext selectionContext = new SelectionContext();
                                    selectionContext.setSelectedAssets(assetsToDelete);
                                    selectionContext.setSelectedContainers(containersToDelete);

                                    RestTemplate restTemplate = new RestTemplate();

                                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                                    HttpEntity<SelectionContext> selectedAssetsEntity = new HttpEntity<>(selectionContext, headers);

                                    String loadbalancerUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, commonObjectsLoadBalancerServiceName);

                                    try{
                                        restTemplate.postForEntity(loadbalancerUrl + "/common-objects/delete", selectedAssetsEntity, GenericResponse.class);
                                        genericResponse.setMessage("Asset was successfully reverted to version " + revertAssetVersionRequest.getVersion() + ".");

                                        // Send notification
                                        String notification = "Asset '" + asset.getName() + "' is reverted to version '" + revertAssetVersionRequest.getVersion() + "' by '" + user.getUsername() + "'";
                                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                        sendNotificationRequest.setIsAsset(true);
                                        sendNotificationRequest.setId(asset.getId());
                                        sendNotificationRequest.setFromUser(user);
                                        sendNotificationRequest.setMessage(notification);
                                        NotificationUtils.getInstance().sendNotification(sendNotificationRequest, discoveryClient, session, notificationsLoadBalancerServiceName);

                                        return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                    }
                                    catch (HttpStatusCodeException httpEx){
                                        JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
                                        throw new Exception("Higher version assets failed to be set as deleted. " + responseJson.get("message").getAsString());
                                    }
                                }
                                else{
                                    String errorMessage = "There were no related assets with version higher than '" + revertAssetVersionRequest.getVersion() + "'.";
                                    _logger.error(errorMessage);

                                    genericResponse.setMessage(errorMessage);

                                    return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                                }
                            }
                            else{
                                throw new IllegalArgumentException("No assets with original asset ID '" + asset.getOriginalAssetId() + "' found!");
                            }
                        }
                        else{
                            throw new IllegalArgumentException("User is null!");
                        }
                    }
                    else{
                        String errorMessage = "Revert asset version request is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Asset ID is blank!";
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
            if(revertAssetVersionRequest != null){
                errorMessage = "An error occurred while reverting the asset with ID '" + assetId + "' to version " + revertAssetVersionRequest.getVersion() + ". " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "An error occurred while reverting the asset with ID '" + assetId + "'. " + e.getLocalizedMessage();
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}