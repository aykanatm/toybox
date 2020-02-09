package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.models.search.FacetField;
import com.github.murataykanat.toybox.models.share.SharedAssets;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.schema.asset.*;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
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
    private FacetUtils facetUtils;
    @Autowired
    private SortUtils sortUtils;
    @Autowired
    private ShareUtils shareUtils;

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private AssetUserRepository assetUserRepository;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @Value("${importStagingPath}")
    private String importStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/assets/upload", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> uploadAssets(Authentication authentication, HttpSession session, @RequestBody UploadFileLst uploadFileLst) {
        try{
            GenericResponse genericResponse = new GenericResponse();
            if(authenticationUtils.isSessionValid(authentication)){
                if(uploadFileLst != null){
                    User user = authenticationUtils.getUser(authentication);
                    if(user != null){
                        boolean canEdit = true;

                        if(shareUtils.isContainerSharedWithUser(user.getId(), uploadFileLst.getContainerId())){
                            List<InternalShare> internalSharesWithTargetUser = shareUtils.getInternalSharesWithTargetUser(user.getId(), uploadFileLst.getContainerId(), false);

                            for(InternalShare internalShare: internalSharesWithTargetUser){
                                canEdit = canEdit && internalShare.getCanEdit().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES);
                            }
                        }

                        if(canEdit){
                            RestTemplate restTemplate = new RestTemplate();

                            HttpHeaders headers = authenticationUtils.getHeaders(session);
                            HttpEntity<UploadFileLst> selectedAssetsEntity = new HttpEntity<>(uploadFileLst, headers);
                            String jobServiceUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.JOB_SERVICE_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.JOB_SERVICE_NAME, session, false);

                            try{
                                restTemplate.postForEntity(jobServiceUrl + "/jobs/import", selectedAssetsEntity, JobResponse.class);
                                genericResponse.setMessage("Import job started for the uploaded file. You can track the result of the import job in 'Jobs' section.");
                                return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                            }
                            catch (HttpStatusCodeException httpEx){
                                JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
                                throw new Exception("Upload was successful but import failed to start. " + responseJson.get("message").getAsString());
                            }
                        }
                        else{
                            String errorMessage = "You do not have permission to edit the folder with ID '" + uploadFileLst.getContainerId() + "'!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(genericResponse, HttpStatus.FORBIDDEN);
                        }
                    }
                    else{
                        throw new IllegalArgumentException("User is null!");
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(assetSearchRequest != null){
                    User user = authenticationUtils.getUser(authentication);
                    if(user != null){
                        String sortColumn = assetSearchRequest.getSortColumn();
                        String sortType = assetSearchRequest.getSortType();

                        int offset = assetSearchRequest.getOffset();
                        int limit = assetSearchRequest.getLimit();

                        List<SearchCondition> searchConditions = assetSearchRequest.getSearchConditions();
                        if(searchConditions == null){
                            searchConditions = new ArrayList<>();
                        }

                        List<SearchRequestFacet> searchRequestFacetList = assetSearchRequest.getAssetSearchRequestFacetList();

                        for (SearchRequestFacet searchRequestFacet: searchRequestFacetList){
                            String fieldName = searchRequestFacet.getFieldName();
                            FacetField facetField = facetUtils.getFacetField(fieldName, new Asset());

                            String dbFieldName = facetField.getFieldName();
                            String fieldValue = searchRequestFacet.getFieldValue();
                            searchConditions.add(new SearchCondition(dbFieldName, ToyboxConstants.SEARCH_CONDITION_EQUALS, fieldValue, facetField.getDataType(), ToyboxConstants.SEARCH_OPERATOR_AND));
                        }

                        List<Asset> allAssets = assetUtils.getAssets(searchConditions);

                        List<SharedAssets> sharedAssetsLst = shareUtils.getSharedAssets(user.getId());

                        if(!allAssets.isEmpty()){
                            List<Asset> assetsByCurrentUser = new ArrayList<>();

                            if(authenticationUtils.isAdminUser(authentication)){
                                _logger.debug("Retrieving all assets [Admin User]...");
                                assetsByCurrentUser = allAssets.stream()
                                        .filter(asset -> asset.getIsLatestVersion().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES))
                                        .collect(Collectors.toList());

                                assetsByCurrentUser.forEach(asset -> {
                                    asset.setCanShare(ToyboxConstants.LOOKUP_YES);
                                    asset.setCanDownload(ToyboxConstants.LOOKUP_YES);
                                    asset.setCanCopy(ToyboxConstants.LOOKUP_YES);
                                    asset.setCanEdit(ToyboxConstants.LOOKUP_YES);
                                });
                            }
                            else{
                                _logger.debug("Retrieving assets of the user '" + user.getUsername() + "'...");

                                for(Asset asset: allAssets){
                                    if(StringUtils.isNotBlank(asset.getImportedByUsername()) && asset.getIsLatestVersion().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES)){
                                        Asset originalAsset = assetUtils.getAsset(asset.getOriginalAssetId());
                                        boolean assetImportedByUser = asset.getImportedByUsername().equalsIgnoreCase(user.getUsername());
                                        boolean originalAssetImportedByUser = originalAsset.getImportedByUsername().equalsIgnoreCase(user.getUsername());
                                        boolean assetIsSharedWithUser = false;

                                        asset.setShared(ToyboxConstants.LOOKUP_NO);

                                        asset.setCanShare(ToyboxConstants.LOOKUP_YES);
                                        asset.setCanDownload(ToyboxConstants.LOOKUP_YES);
                                        asset.setCanCopy(ToyboxConstants.LOOKUP_YES);
                                        asset.setCanEdit(ToyboxConstants.LOOKUP_YES);

                                        for(SharedAssets sharedAssets: sharedAssetsLst){
                                            assetIsSharedWithUser = sharedAssets.getAssetIds().stream().anyMatch(assetId -> assetId.equalsIgnoreCase(asset.getId()));
                                            if(assetIsSharedWithUser){
                                                asset.setCanShare(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_SHARE, user.getId(), asset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                                asset.setCanEdit(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_EDIT, user.getId(), asset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                                asset.setCanCopy(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_COPY, user.getId(), asset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                                asset.setCanDownload(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_DOWNLOAD, user.getId(), asset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                                asset.setShared(ToyboxConstants.LOOKUP_YES);
                                                asset.setSharedByUsername(sharedAssets.getUsername());
                                                break;
                                            }
                                        }

                                        if(assetImportedByUser || originalAssetImportedByUser || assetIsSharedWithUser){
                                            assetsByCurrentUser.add(asset);
                                        }
                                    }
                                }
                            }

                            // Set facets
                            List<Facet> facets = facetUtils.getFacets(assetsByCurrentUser);
                            retrieveAssetsResults.setFacets(facets);

                            // Sort assets
                            if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
                                sortUtils.sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                                sortUtils.sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            }

                            // Paginate assets
                            int totalRecords = assetsByCurrentUser.size();
                            if(offset > totalRecords){
                                offset = 0;
                            }
                            int endIndex = Math.min((offset + limit), totalRecords);

                            List<Asset> assetsOnPage = assetsByCurrentUser.subList(offset, endIndex);

                            // Set subscription status
                            List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());

                            for(Asset assetOnPage: assetsOnPage){
                                assetOnPage.setSubscribed(ToyboxConstants.LOOKUP_NO);

                                boolean isSubscribedAsset = assetUsersByUserId.stream().anyMatch(assetUser -> assetUser.getAssetId().equalsIgnoreCase(assetOnPage.getId()));
                                if(isSubscribedAsset){
                                    assetOnPage.setSubscribed(ToyboxConstants.LOOKUP_YES);
                                    break;
                                }
                            }

                            // Set parent container IDs
                            for(Asset assetOnPage: assetsOnPage){
                                String parentContainerId = containerUtils.getParentContainerId(assetOnPage);
                                assetOnPage.setParentContainerId(parentContainerId);
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(updateAssetRequest != null){
                        User user = authenticationUtils.getUser(authentication);
                        if(user != null){
                            boolean canEdit = shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_EDIT, user.getId(), assetId, true);
                            if(canEdit){
                                Asset asset = assetUtils.updateAsset(updateAssetRequest, assetId, user, session);
                                if(asset != null){
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
                                String errorMessage = "You do not have permission to edit the selected file!";
                                _logger.error(errorMessage);

                                genericResponse.setMessage(errorMessage);

                                return new ResponseEntity<>(genericResponse, HttpStatus.FORBIDDEN);
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
            String errorMessage = "An error occurred while updating an asset. " + e.getLocalizedMessage();
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    User user = authenticationUtils.getUser(authentication);
                    if(user != null){
                        boolean canEdit = shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_EDIT, user.getId(), assetId, true);
                        if(canEdit){
                            Asset asset = assetUtils.getAsset(assetId);

                            List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                            if(!assetsByOriginalAssetId.isEmpty()){
                                sortUtils.sortItems("des", assetsByOriginalAssetId, Comparator.comparing(Asset::getVersion));

                                assetVersionResponse.setAssets(assetsByOriginalAssetId);
                                assetVersionResponse.setMessage("Asset version history retrieved successfully.");

                                return new ResponseEntity<>(assetVersionResponse, HttpStatus.OK);
                            }
                            else{
                                throw new IllegalArgumentException("No assets found with original asset ID '" + asset.getOriginalAssetId() + "'!");
                            }
                        }
                        else{
                            String errorMessage = "You do not have permission to edit the selected file!";
                            _logger.error(errorMessage);

                            assetVersionResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(assetVersionResponse, HttpStatus.FORBIDDEN);
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(revertAssetVersionRequest != null){
                        User user = authenticationUtils.getUser(authentication);
                        if(user != null){
                            Asset asset = assetUtils.getAsset(assetId);
                            List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                            if(!assetsByOriginalAssetId.isEmpty()){
                                sortUtils.sortItems("des", assetsByOriginalAssetId, Comparator.comparing(Asset::getVersion));
                                List<Asset> assetsToDelete = new ArrayList<>();
                                for(Asset versionAsset: assetsByOriginalAssetId){
                                    if(versionAsset.getVersion() > revertAssetVersionRequest.getVersion()){
                                        assetsToDelete.add(versionAsset);
                                    }
                                    else if(versionAsset.getVersion() == revertAssetVersionRequest.getVersion()){
                                        List<String> assetIds = new ArrayList<>();
                                        assetIds.add(versionAsset.getId());

                                        assetsRepository.updateAssetsLatestVersion(ToyboxConstants.LOOKUP_YES, assetIds);
                                    }
                                }

                                if(!assetsToDelete.isEmpty()){
                                    assetsRepository.updateAssetsLatestVersion(ToyboxConstants.LOOKUP_NO, assetsToDelete.stream().map(Asset::getId).collect(Collectors.toList()));
                                    List<Container> containersToDelete = new ArrayList<>();

                                    SelectionContext selectionContext = new SelectionContext();
                                    selectionContext.setSelectedAssets(assetsToDelete);
                                    selectionContext.setSelectedContainers(containersToDelete);

                                    RestTemplate restTemplate = new RestTemplate();

                                    HttpHeaders headers = authenticationUtils.getHeaders(session);
                                    HttpEntity<SelectionContext> selectedAssetsEntity = new HttpEntity<>(selectionContext, headers);

                                    String loadbalancerUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.COMMON_OBJECTS_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.COMMON_OBJECT_SERVICE_NAME, session, false);

                                    try{
                                        restTemplate.postForEntity(loadbalancerUrl + "/common-objects/delete", selectedAssetsEntity, GenericResponse.class);
                                        genericResponse.setMessage("Asset was successfully reverted to version " + revertAssetVersionRequest.getVersion() + ".");

                                        // Send notification for subscribers
                                        List<User> subscribers = assetUtils.getSubscribers(asset.getId());
                                        for(User subscriber: subscribers){
                                            String message = "Asset '" + asset.getName() + "' is reverted to version '" + revertAssetVersionRequest.getVersion() + "' by '" + user.getUsername() + "'";
                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                            sendNotificationRequest.setFromUsername(user.getUsername());
                                            sendNotificationRequest.setToUsername(subscriber.getUsername());
                                            sendNotificationRequest.setMessage(message);
                                            notificationUtils.sendNotification(sendNotificationRequest, session);
                                        }

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