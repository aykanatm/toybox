package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.models.search.FacetField;
import com.github.murataykanat.toybox.models.share.SharedAssets;
import com.github.murataykanat.toybox.models.share.SharedContainers;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.container.*;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.github.murataykanat.toybox.utilities.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class FolderController {
    private static final Log _logger = LogFactory.getLog(FolderController.class);

    @Autowired
    private ContainerUtils containerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private NotificationUtils notificationUtils;
    @Autowired
    private FacetUtils facetUtils;
    @Autowired
    private ShareUtils shareUtils;
    @Autowired
    private AssetUtils assetUtils;

    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;
    @Autowired
    private ContainerUsersRepository containerUsersRepository;
    @Autowired
    private AssetUserRepository assetUserRepository;

    @Value("${toyboxSuperAdminUsername}")
    private String toyboxSuperAdminUsername;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/containers", method = RequestMethod.POST)
    public ResponseEntity<CreateContainerResponse> createContainer(Authentication authentication, HttpSession session, @RequestBody CreateContainerRequest createContainerRequest){
        CreateContainerResponse createContainerResponse = new CreateContainerResponse();

        try {
            if(authenticationUtils.isSessionValid(authentication)){
                if(createContainerRequest != null){
                    if(StringUtils.isNotBlank(createContainerRequest.getContainerName())){
                        User user = authenticationUtils.getUser(authentication);
                        if(user != null){
                            boolean canCreateFolder;
                            boolean canSendNotification = false;
                            Container parentContainer = null;

                            String errorMessage = "";
                            if(StringUtils.isBlank(createContainerRequest.getParentContainerId())){
                                if(authenticationUtils.isAdminUser(authentication)){
                                    Container duplicateTopLevelContainer = containerUtils.findDuplicateTopLevelContainer(createContainerRequest.getContainerName());
                                    if(duplicateTopLevelContainer == null){
                                        canCreateFolder = true;
                                    }
                                    else{
                                        canCreateFolder = false;
                                        errorMessage = "The root folder already has a folder named '" + createContainerRequest.getContainerName() + "'.";
                                    }

                                }
                                else{
                                    canCreateFolder = false;
                                    errorMessage = "You are not allowed to create a folder under the root folder.";
                                }
                            }
                            else{
                                boolean canEdit = true;

                                if(shareUtils.isContainerSharedWithUser(user.getId(), createContainerRequest.getParentContainerId())){
                                    List<InternalShare> internalSharesWithTargetUser = shareUtils.getInternalSharesWithTargetUser(user.getId(), createContainerRequest.getParentContainerId(), false);

                                    for(InternalShare internalShare: internalSharesWithTargetUser){
                                        canEdit = canEdit && internalShare.getCanEdit().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES);
                                    }
                                }

                                parentContainer = containerUtils.getContainer(createContainerRequest.getParentContainerId());

                                if(canEdit){
                                    Container duplicateContainer = containerUtils.findDuplicateContainer(createContainerRequest.getParentContainerId(), createContainerRequest.getContainerName());
                                    if(duplicateContainer == null){
                                        canCreateFolder = true;
                                        canSendNotification = true;
                                    }
                                    else{
                                        canCreateFolder = false;
                                        errorMessage = "The folder '" + parentContainer.getName() + "' already has a folder named '" + createContainerRequest.getContainerName() + "'.";
                                    }
                                }
                                else{
                                    canCreateFolder = false;
                                    errorMessage = "You are not allowed to create a folder under the folder '" + parentContainer.getName() + "'.";
                                }
                            }

                            if(canCreateFolder){
                                Container container = new Container();
                                container.setName(createContainerRequest.getContainerName());
                                container.setParentId(createContainerRequest.getParentContainerId());
                                container.setCreatedByUsername(authentication.getName());
                                container.setImportDate(Calendar.getInstance().getTime());
                                container.setDeleted(ToyboxConstants.LOOKUP_NO);
                                container.setSystem(ToyboxConstants.LOOKUP_NO);

                                containerUtils.createContainer(container);

                                // Check if the parent folder is shared
                                // If so add the folder to the shares
                                List<InternalShare> internalShares = shareUtils.getInternalSharesContainingItem(container.getParentId(), false);
                                for(InternalShare internalShare: internalShares){
                                    shareUtils.addContainerToInternalShare(container.getId(), internalShare.getId());
                                }

                                createContainerResponse.setContainerId(container.getId());
                                createContainerResponse.setMessage("Folder created successfully!");

                                if(canSendNotification){
                                    // Send notification for subscribers
                                    List<User> subscribers = containerUtils.getSubscribers(container.getId());
                                    for(User subscriber: subscribers){
                                        String message = "Folder '" + container.getName() + "' is created under the folder '" + parentContainer.getName() + "' by '" + user.getUsername() + "'";
                                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                        sendNotificationRequest.setFromUsername(user.getUsername());
                                        sendNotificationRequest.setToUsername(subscriber.getUsername());
                                        sendNotificationRequest.setMessage(message);
                                        notificationUtils.sendNotification(sendNotificationRequest, session);
                                    }
                                }

                                return new ResponseEntity<>(createContainerResponse, HttpStatus.OK);
                            }
                            else{
                                _logger.error(errorMessage);

                                createContainerResponse.setMessage(errorMessage);

                                return new ResponseEntity<>(createContainerResponse, HttpStatus.FORBIDDEN);
                            }
                        }
                        else{
                            throw new IllegalArgumentException("User is null!");
                        }
                    }
                    else{
                        String errorMessage = "Container name is blank!";
                        _logger.debug(errorMessage);

                        createContainerResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(createContainerResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Create container request is null!";
                    _logger.debug(errorMessage);

                    createContainerResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(createContainerResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                createContainerResponse.setMessage(errorMessage);

                return new ResponseEntity<>(createContainerResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while creating the container. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            createContainerResponse.setMessage(errorMessage);

            return new ResponseEntity<>(createContainerResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/containers/{containerId}/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainerContentsResult> retrieveContainerContents(Authentication authentication, @PathVariable String containerId, @RequestBody AssetSearchRequest assetSearchRequest){
        RetrieveContainerContentsResult retrieveContainerContentsResult = new RetrieveContainerContentsResult();
        try{
            if(!authenticationUtils.isSessionValid(authentication)){
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveContainerContentsResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.UNAUTHORIZED);
            }

            if(StringUtils.isBlank(containerId)){
                String errorMessage = "Container ID is blank!";
                _logger.debug(errorMessage);

                retrieveContainerContentsResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
            }

            if(assetSearchRequest == null){
                String errorMessage = "Asset search request is null!";
                _logger.debug(errorMessage);

                retrieveContainerContentsResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
            }

            User user = authenticationUtils.getUser(authentication);
            if(user == null){
                throw new IllegalArgumentException("User is null!");
            }

            String sortField = assetSearchRequest.getSortColumn();
            String sortType = assetSearchRequest.getSortType();

            int offset = assetSearchRequest.getOffset();
            int limit = assetSearchRequest.getLimit();

            List<SearchCondition> searchConditions = assetSearchRequest.getSearchConditions();
            if(searchConditions == null){
                searchConditions = new ArrayList<>();
            }

            List<SearchCondition> assetSearchConditions = new ArrayList<>(searchConditions);
            List<SearchCondition> containerSearchConditions = new ArrayList<>(searchConditions);

            List<SearchRequestFacet> searchRequestFacetList = assetSearchRequest.getAssetSearchRequestFacetList();

            for (SearchRequestFacet searchRequestFacet: searchRequestFacetList){
                String fieldName = searchRequestFacet.getFieldName();

                FacetField facetField;

                try{
                    facetField = facetUtils.getFacetField(fieldName, new Asset());

                    String dbFieldName = facetField.getFieldName();
                    String fieldValue = searchRequestFacet.getFieldValue();
                    assetSearchConditions.add(new SearchCondition(dbFieldName, ToyboxConstants.SEARCH_CONDITION_EQUALS,
                            fieldValue, facetField.getDataType(), ToyboxConstants.SEARCH_OPERATOR_AND));
                }
                catch (IllegalArgumentException e){
                    _logger.debug(e.getLocalizedMessage() + ". Trying the next valid object...");
                    facetField = facetUtils.getFacetField(fieldName, new Container());

                    String dbFieldName = facetField.getFieldName();
                    String fieldValue = searchRequestFacet.getFieldValue();
                    containerSearchConditions.add(new SearchCondition(dbFieldName, ToyboxConstants.SEARCH_CONDITION_EQUALS,
                            fieldValue, facetField.getDataType(), ToyboxConstants.SEARCH_OPERATOR_AND));
                }
            }

            if(containerId.equalsIgnoreCase("root") && !authenticationUtils.isAdminUser(authentication)){
                containerId = user.getUsername();
            }

            List<Asset> assetsByCurrentUser = new ArrayList<>();
            List<Container> containersByCurrentUser;
            if(!containerId.equalsIgnoreCase("root")){
                Container container = containerUtils.getContainer(containerId);

                List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(container.getId());
                List<String> containerAssetIdsByContainerId = containerAssetsByContainerId.stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());

                assetSearchConditions.add(new SearchCondition("isLatestVersion", ToyboxConstants.SEARCH_CONDITION_EQUALS, ToyboxConstants.LOOKUP_YES,
                        ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_AND));

                for(String assetId : containerAssetIdsByContainerId){
                    assetSearchConditions.add(new SearchCondition("id", ToyboxConstants.SEARCH_CONDITION_EQUALS, assetId,
                            ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_AND_IN));
                }

                containerSearchConditions.add(new SearchCondition("parentId", ToyboxConstants.SEARCH_CONDITION_EQUALS, container.getId(),
                        ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_AND));

                boolean isUserMainContainer = (container.getCreatedByUsername().equalsIgnoreCase(toyboxSuperAdminUsername) && container.getName().equalsIgnoreCase(user.getUsername()));
                if(isUserMainContainer){
                    // If we are in the main container add the assets which are shared by other users and in their main container
                    List<SharedAssets> sharedAssetsLst = shareUtils.getSharedAssets(user.getId());
                    for(SharedAssets sharedAssets: sharedAssetsLst){
                        List<String> assetIds = sharedAssets.getAssetIds();
                        for(String assetId: assetIds){
                            List<ContainerAsset> containerAssetsByAssetId = containerAssetsRepository.findContainerAssetsByAssetId(assetId);
                            if(!containerAssetsByAssetId.isEmpty()){
                                if(containerAssetsByAssetId.size() == 1){
                                    ContainerAsset containerAsset = containerAssetsByAssetId.get(0);
                                    Container assetContainer = containerUtils.getContainer(containerAsset.getContainerId());
                                    if(assetContainer.getSystem().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES)){
                                        assetSearchConditions.add(new SearchCondition("id", ToyboxConstants.SEARCH_CONDITION_EQUALS, assetId,
                                                ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_AND_IN));
                                    }
                                }
                                else{
                                    throw new IllegalArgumentException("Asset with ID '" + assetId + "' is in multiple containers!");
                                }
                            }
                            else{
                                throw new IllegalArgumentException("Asset with ID '" + assetId + "' is not in any container!");
                            }
                        }
                    }

                    assetsByCurrentUser = assetUtils.getAssets(assetSearchConditions, sortField, sortType);

                    List<SharedContainers> sharedContainersLst = shareUtils.getSharedContainers(user.getId());
                    for(SharedContainers sharedContainers: sharedContainersLst){
                        List<Container> containersByContainerIds = containerUtils.getContainersByContainerIds(sharedContainers.getContainerIds());

                        // If the container's parent container is on the list, exclude it
                        for(Container containerToAdd: containersByContainerIds){
                            boolean addContainer = true;

                            for(Container containerToCheck: containersByContainerIds){
                                if(containerToAdd.getParentId().equalsIgnoreCase(containerToCheck.getId())){
                                    addContainer = false;
                                    break;
                                }
                            }

                            if(addContainer){
                                String keyword = containerSearchConditions.stream()
                                        .filter(csc -> csc.getField().equalsIgnoreCase("name"))
                                        .map(SearchCondition::getKeyword).findFirst()
                                        .orElse(null);
                                if(StringUtils.isNotBlank(keyword)){
                                    containerSearchConditions.add(
                                            new SearchCondition("id", ToyboxConstants.SEARCH_CONDITION_EQUALS, containerToAdd.getId(),
                                                    ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_OR_IN,
                                                    new SearchCondition("name", ToyboxConstants.SEARCH_CONDITION_CONTAINS, keyword,
                                                            ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_AND)));
                                }
                                else{
                                    containerSearchConditions.add(
                                            new SearchCondition("id", ToyboxConstants.SEARCH_CONDITION_EQUALS, containerToAdd.getId(),
                                                    ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_OR_IN));
                                }
                            }
                        }
                    }
                }
                else{
                    if(!containerAssetIdsByContainerId.isEmpty()){
                        assetsByCurrentUser = assetUtils.getAssets(assetSearchConditions, sortField, sortType);
                    }
                }

                for(Asset userAsset: assetsByCurrentUser){
                    if(!authenticationUtils.isAdminUser(authentication)){
                        boolean assetSharedWithUser = shareUtils.isAssetSharedWithUser(user.getId(), userAsset.getId());
                        if(assetSharedWithUser){
                            User sourceUser = shareUtils.getSourceUser(user.getId(), userAsset.getId(), true);
                            if(sourceUser != null){
                                userAsset.setCanShare(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_SHARE, user.getId(), userAsset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                userAsset.setCanEdit(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_EDIT, user.getId(), userAsset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                userAsset.setCanCopy(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_COPY, user.getId(), userAsset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                userAsset.setCanDownload(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_DOWNLOAD, user.getId(), userAsset.getId(), true) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                                userAsset.setShared(ToyboxConstants.LOOKUP_YES);
                                userAsset.setSharedByUsername(sourceUser.getUsername());
                            }
                            else{
                                throw new IllegalArgumentException("Source user is null!");
                            }
                        }
                        else{
                            userAsset.setShared(ToyboxConstants.LOOKUP_NO);

                            userAsset.setCanDownload(ToyboxConstants.LOOKUP_YES);
                            userAsset.setCanCopy(ToyboxConstants.LOOKUP_YES);
                            userAsset.setCanEdit(ToyboxConstants.LOOKUP_YES);
                            userAsset.setCanShare(ToyboxConstants.LOOKUP_YES);
                        }
                    }
                    else{
                        userAsset.setCanDownload(ToyboxConstants.LOOKUP_YES);
                        userAsset.setCanCopy(ToyboxConstants.LOOKUP_YES);
                        userAsset.setCanEdit(ToyboxConstants.LOOKUP_YES);
                        userAsset.setCanShare(ToyboxConstants.LOOKUP_YES);
                    }
                }

                // Set breadcrumbs
                retrieveContainerContentsResult.setBreadcrumbs(containerUtils.generateContainerPath(container.getId()));
            }
            else{
                containerSearchConditions.add(new SearchCondition("parentId", ToyboxConstants.SEARCH_CONDITION_IS_NULL, null,
                        ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING, ToyboxConstants.SEARCH_OPERATOR_AND));

                // Set breadcrumbs
                retrieveContainerContentsResult.setBreadcrumbs(containerUtils.generateContainerPath(null));
            }

            containersByCurrentUser = containerUtils.getContainers(containerSearchConditions, sortField, sortType);

            for(Container userContainer: containersByCurrentUser){
                if(!authenticationUtils.isAdminUser(authentication)){
                    boolean containerSharedWithUser = shareUtils.isContainerSharedWithUser(user.getId(), userContainer.getId());
                    if(containerSharedWithUser){
                        User sourceUser = shareUtils.getSourceUser(user.getId(), userContainer.getId(), false);
                        if(sourceUser != null){
                            userContainer.setCanShare(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_SHARE, user.getId(), userContainer.getId(), false) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                            userContainer.setCanEdit(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_EDIT, user.getId(), userContainer.getId(), false) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                            userContainer.setCanCopy(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_COPY, user.getId(), userContainer.getId(), false) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                            userContainer.setCanDownload(shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_DOWNLOAD, user.getId(), userContainer.getId(), false) ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
                            userContainer.setShared(ToyboxConstants.LOOKUP_YES);
                            userContainer.setSharedByUsername(sourceUser.getUsername());
                        }
                        else{
                            throw new IllegalArgumentException("Source user is null!");
                        }
                    }
                    else{
                        userContainer.setShared(ToyboxConstants.LOOKUP_NO);

                        userContainer.setCanShare(ToyboxConstants.LOOKUP_YES);
                        userContainer.setCanDownload(ToyboxConstants.LOOKUP_YES);
                        userContainer.setCanCopy(ToyboxConstants.LOOKUP_YES);
                        userContainer.setCanEdit(ToyboxConstants.LOOKUP_YES);
                    }
                }
                else{
                    userContainer.setCanShare(ToyboxConstants.LOOKUP_YES);
                    userContainer.setCanDownload(ToyboxConstants.LOOKUP_YES);
                    userContainer.setCanCopy(ToyboxConstants.LOOKUP_YES);
                    userContainer.setCanEdit(ToyboxConstants.LOOKUP_YES);
                }
            }

            // Set facets
            List<Facet> assetFacets = facetUtils.getFacets(assetsByCurrentUser);
            List<Facet> containerFacets = facetUtils.getFacets(containersByCurrentUser);

            List<Facet> commonFacets = facetUtils.getCommonFacets(assetFacets, containerFacets);

            retrieveContainerContentsResult.setFacets(commonFacets);

            // Merge containers and assets
            List<ContainerItem> containerItems = new ArrayList<>(containersByCurrentUser);
            containerItems.addAll(assetsByCurrentUser);

            // Paginate results
            int totalRecords = containerItems.size();
            if(offset > totalRecords){
                offset = 0;
            }
            int endIndex = Math.min((offset + limit), totalRecords);

            List<ContainerItem> containerItemsOnPage = containerItems.subList(offset, endIndex);

            // Set subscription status
            List<ContainerUser> containerUsersByUserId = containerUsersRepository.findContainerUsersByUserId(user.getId());
            List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());

            for(ContainerItem containerItem: containerItemsOnPage){
                Class<? extends ContainerItem> containerItemClass = containerItem.getClass();

                if(containerItemClass.getName().equalsIgnoreCase("com.github.murataykanat.toybox.dbo.Container")){
                    Container containerOnPage = (Container) containerItem;

                    containerOnPage.setSubscribed(ToyboxConstants.LOOKUP_NO);
                    for(ContainerUser containerUser: containerUsersByUserId){
                        if(containerOnPage.getId().equalsIgnoreCase(containerUser.getContainerId())){
                            containerOnPage.setSubscribed(ToyboxConstants.LOOKUP_YES);
                            break;
                        }
                    }
                }
                else if(containerItemClass.getName().equalsIgnoreCase("com.github.murataykanat.toybox.dbo.Asset")){
                    Asset assetOnPage = (Asset) containerItem;

                    assetOnPage.setSubscribed(ToyboxConstants.LOOKUP_NO);
                    for(AssetUser assetUser: assetUsersByUserId){
                        if(assetOnPage.getId().equalsIgnoreCase(assetUser.getAssetId())){
                            assetOnPage.setSubscribed(ToyboxConstants.LOOKUP_YES);
                            break;
                        }
                    }

                    // Set parent container ID
                    List<ContainerAsset> containerAssetsByAssetId = containerAssetsRepository.findContainerAssetsByAssetId(assetOnPage.getId());
                    if(!containerAssetsByAssetId.isEmpty()){
                        if(containerAssetsByAssetId.size() == 1){
                            ContainerAsset containerAsset = containerAssetsByAssetId.get(0);
                            assetOnPage.setParentContainerId(containerAsset.getContainerId());
                        }
                        else{
                            throw new IllegalArgumentException("Asset is in multiple folders!");
                        }
                    }
                    else{
                        throw new IllegalArgumentException("Asset is not in any folder!");
                    }
                }
            }

            // Set edit status
            if(shareUtils.isContainerSharedWithUser(user.getId(), containerId)){
                List<InternalShare> internalSharesWithTargetUser = shareUtils.getInternalSharesWithTargetUser(user.getId(), containerId, false);
                boolean canEdit = true;
                for(InternalShare internalShare: internalSharesWithTargetUser){
                    canEdit = canEdit && internalShare.getCanEdit().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES);
                }

                retrieveContainerContentsResult.setCanEdit(canEdit ? ToyboxConstants.LOOKUP_YES : ToyboxConstants.LOOKUP_NO);
            }
            else{
                retrieveContainerContentsResult.setCanEdit(ToyboxConstants.LOOKUP_YES);
            }

            // Finalize
            retrieveContainerContentsResult.setTotalRecords(totalRecords);
            retrieveContainerContentsResult.setContainerItems(containerItemsOnPage);
            retrieveContainerContentsResult.setMessage("Container contents retrieved successfully!");

            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.OK);
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving items inside the container with ID '" + containerId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveContainerContentsResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/containers/{containerId}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateContainer(Authentication authentication, HttpSession session, @RequestBody UpdateContainerRequest updateContainerRequest, @PathVariable String containerId){
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
               if(StringUtils.isNotBlank(containerId)){
                    if(updateContainerRequest != null){
                        User user = authenticationUtils.getUser(authentication);
                        if(user != null){
                            boolean canEdit = shareUtils.hasPermission(ToyboxConstants.SHARE_PERMISSION_EDIT, user.getId(), containerId, false);
                            if(canEdit){
                                Container container = containerUtils.updateContainer(updateContainerRequest, containerId, user, session);
                                if(container != null){
                                    String message = "Container updated successfully.";
                                    _logger.debug(message);

                                    genericResponse.setMessage(message);

                                    return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                }
                                else{
                                    throw new IllegalArgumentException("Container update failed!");
                                }
                            }
                            else{
                                String errorMessage = "You do not have permission to edit the selected folder!";
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
                        String errorMessage = "Update container request is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                   String errorMessage = "Container ID is blank!";
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
            String errorMessage = "An error occurred while updating the container with ID '" + containerId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}