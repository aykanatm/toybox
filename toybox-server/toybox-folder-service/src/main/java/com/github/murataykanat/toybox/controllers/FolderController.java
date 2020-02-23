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
    private SortUtils sortUtils;
    @Autowired
    private ShareUtils shareUtils;
    @Autowired
    private AssetUtils assetUtils;

    @Autowired
    private ContainersRepository containersRepository;
    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;
    @Autowired
    private ContainerUsersRepository containerUsersRepository;
    @Autowired
    private AssetsRepository assetsRepository;
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(containerId)){
                    if(assetSearchRequest != null){
                        Container container = containerUtils.getContainer(containerId);
                        User user = authenticationUtils.getUser(authentication);
                        if(user != null){
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
                                    assetSearchConditions.add(new SearchCondition(dbFieldName, ToyboxConstants.SEARCH_CONDITION_EQUALS, fieldValue, facetField.getDataType(), ToyboxConstants.SEARCH_OPERATOR_AND));
                                }
                                catch (IllegalArgumentException e){
                                    _logger.debug(e.getLocalizedMessage() + ". Trying the next valid object...");
                                    facetField = facetUtils.getFacetField(fieldName, new Container());

                                    String dbFieldName = facetField.getFieldName();
                                    String fieldValue = searchRequestFacet.getFieldValue();
                                    containerSearchConditions.add(new SearchCondition(dbFieldName, ToyboxConstants.SEARCH_CONDITION_EQUALS, fieldValue, facetField.getDataType(), ToyboxConstants.SEARCH_OPERATOR_AND));
                                }
                            }

                            List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(container.getId());
                            List<String> containerAssetIdsByContainerId = containerAssetsByContainerId.stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());

                            SearchCondition asc1 = new SearchCondition();
                            asc1.setBooleanOperator(ToyboxConstants.SEARCH_OPERATOR_AND);
                            asc1.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING);
                            asc1.setField("isLatestVersion");
                            asc1.setKeyword(ToyboxConstants.LOOKUP_YES);
                            asc1.setOperator(ToyboxConstants.SEARCH_CONDITION_EQUALS);
                            assetSearchConditions.add(asc1);

                            boolean firstAsset = true;

                            for(String assetId : containerAssetIdsByContainerId){
                                SearchCondition asc = new SearchCondition();
                                if(firstAsset){
                                    asc.setBooleanOperator(ToyboxConstants.SEARCH_OPERATOR_AND);
                                    firstAsset = false;
                                }
                                else{
                                    asc.setBooleanOperator(ToyboxConstants.SEARCH_OPERATOR_OR);
                                }

                                asc.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING);
                                asc.setField("id");
                                asc.setKeyword(assetId);
                                asc.setOperator(ToyboxConstants.SEARCH_CONDITION_EQUALS);

                                assetSearchConditions.add(asc);
                            }

                            SearchCondition csc1 = new SearchCondition();
                            csc1.setBooleanOperator(ToyboxConstants.SEARCH_OPERATOR_AND);
                            csc1.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING);
                            csc1.setField("parentId");
                            csc1.setKeyword(container.getId());
                            csc1.setOperator(ToyboxConstants.SEARCH_CONDITION_EQUALS);

                            containerSearchConditions.add(csc1);

//                            if(!containerAssetIdsByContainerId.isEmpty()){
//                                allAssets = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(containerAssetIdsByContainerId);
//                            }
//                            else{
//                                allAssets = new ArrayList<>();
//                            }

                            List<Container> containersByCurrentUser;
                            List<Asset> assetsByCurrentUser = new ArrayList<>();

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
                                                    SearchCondition asc = new SearchCondition();
                                                    if(firstAsset){
                                                        asc.setBooleanOperator(ToyboxConstants.SEARCH_OPERATOR_AND);
                                                        firstAsset = false;
                                                    }
                                                    else{
                                                        asc.setBooleanOperator(ToyboxConstants.SEARCH_OPERATOR_OR);
                                                    }
                                                    asc.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING);
                                                    asc.setField("id");
                                                    asc.setKeyword(assetId);
                                                    asc.setOperator(ToyboxConstants.SEARCH_CONDITION_EQUALS);

                                                    assetSearchConditions.add(asc);
//                                                    Asset asset = assetUtils.getAsset(assetId);
//                                                    allAssets.add(asset);
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

                                // containersByCurrentUser = containerUtils.getContainers(containerSearchConditions, sortField, sortType);
                                // containersByCurrentUser = containersRepository.getNonDeletedContainersByParentContainerId(container.getId());

                                List<SharedContainers> sharedContainersLst = shareUtils.getSharedContainers(user.getId());
                                for(SharedContainers sharedContainers: sharedContainersLst){
                                    // List<Container> containersToAdd = new ArrayList<>();
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
                                            SearchCondition csc = new SearchCondition();
                                            csc.setBooleanOperator(ToyboxConstants.SEARCH_OPERATOR_OR);
                                            csc.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING);
                                            csc.setField("id");
                                            csc.setKeyword(containerToAdd.getId());
                                            csc.setOperator(ToyboxConstants.SEARCH_CONDITION_EQUALS);

                                            containerSearchConditions.add(csc);

                                            // containersToAdd.add(containerToAdd);
                                        }
                                    }
                                    // containersByCurrentUser.addAll(containersToAdd);
                                }

                                containersByCurrentUser = containerUtils.getContainers(containerSearchConditions, sortField, sortType);
                            }
                            else{
                                if(!containerAssetIdsByContainerId.isEmpty()){
                                    assetsByCurrentUser = assetUtils.getAssets(assetSearchConditions, sortField, sortType);
                                }
                                containersByCurrentUser = containerUtils.getContainers(containerSearchConditions, sortField, sortType);
                                // containersByCurrentUser = containersRepository.getNonDeletedContainersByParentContainerId(container.getId());
                            }

//                            assetsByCurrentUser = allAssets.stream()
//                                    .filter(asset -> asset.getIsLatestVersion().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES))
//                                    .collect(Collectors.toList());



                            // Sort containers
                            // sortUtils.sortItems("asc", containersByCurrentUser, Comparator.comparing(Container::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            // Sort assets
//                            if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
//                                sortUtils.sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
//                            }
//                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
//                                sortUtils.sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
//                            }

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

//                            // Filter assets
//                            if(!searchRequestFacetList.isEmpty()){
//                                assetsByCurrentUser = assetsByCurrentUser.stream().filter(asset -> facetUtils.hasFacetValue(asset, searchRequestFacetList)).collect(Collectors.toList());
//                            }
//                            // Filter containers
//                            if(!searchRequestFacetList.isEmpty()){
//                                containersByCurrentUser = containersByCurrentUser.stream().filter(userContainer -> facetUtils.hasFacetValue(userContainer, searchRequestFacetList)).collect(Collectors.toList());
//                            }

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

                            // Set breadcrumbs
                            retrieveContainerContentsResult.setBreadcrumbs(containerUtils.generateContainerPath(container.getId()));

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
                        else{
                            throw new IllegalArgumentException("User is null!");
                        }
                    }
                    else{
                        String errorMessage = "Asset search request is null!";
                        _logger.debug(errorMessage);

                        retrieveContainerContentsResult.setMessage(errorMessage);

                        return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Container ID is blank!";
                    _logger.debug(errorMessage);

                    retrieveContainerContentsResult.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveContainerContentsResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.UNAUTHORIZED);
            }
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

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/containers/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainersResults> retrieveContainers(Authentication authentication, @RequestBody ContainerSearchRequest containerSearchRequest){
        RetrieveContainersResults retrieveContainersResults = new RetrieveContainersResults();
        try {
            if(authenticationUtils.isSessionValid(authentication)){
                if(containerSearchRequest != null){
                    User user = authenticationUtils.getUser(authentication);
                    if(user != null){
                        int startIndex = containerSearchRequest.getOffset();
                        int limit = containerSearchRequest.getLimit();

                        List<Container> containersByCurrentUser;

                        if(containerSearchRequest.getRetrieveTopLevelContainers() != null && containerSearchRequest.getRetrieveTopLevelContainers().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES)){
                            if(authenticationUtils.isAdminUser(authentication)){
                                _logger.debug("Retrieving the top level containers [Admin User]...");
                                containersByCurrentUser = containersRepository.getTopLevelNonDeletedContainers();
                            }
                            else{
                                String errorMessage = "The user '" + user.getUsername() + "' does not have permissions to retrieve the top level containers!";
                                _logger.error(errorMessage);

                                retrieveContainersResults.setMessage(errorMessage);

                                return new ResponseEntity<>(retrieveContainersResults, HttpStatus.UNAUTHORIZED);
                            }
                        }
                        else{
                            if(authenticationUtils.isAdminUser(authentication)){
                                containersByCurrentUser = containersRepository.getAllNonDeletedContainers();
                            }
                            else{
                                containersByCurrentUser = containersRepository.getUserFolders(user.getUsername());
                            }

                            Container filterByContainer = containerSearchRequest.getContainer();
                            boolean filterById = StringUtils.isNotBlank(filterByContainer.getId());
                            boolean filterByParentId = StringUtils.isNotBlank(filterByContainer.getParentId());
                            boolean filterByName = StringUtils.isNotBlank(filterByContainer.getName());
                            boolean filterByCreatedByUsername = StringUtils.isNotBlank(filterByContainer.getCreatedByUsername());
                            boolean filterByCreationDate = filterByContainer.getImportDate() != null;
                            boolean filterByDeleted = StringUtils.isNotBlank(filterByContainer.getDeleted());
                            boolean filterByIsSystem = StringUtils.isNotBlank(filterByContainer.getSystem());

                            containersByCurrentUser = containersByCurrentUser.stream()
                                    .filter(container ->
                                    (!filterById || container.getId().contains(filterByContainer.getId()))
                                    && (!filterByParentId || container.getParentId().contains(filterByContainer.getParentId()))
                                    && (!filterByName || container.getName().contains(filterByContainer.getName()))
                                    && (!filterByCreatedByUsername || container.getCreatedByUsername().contains(filterByContainer.getCreatedByUsername()))
                                    && (!filterByCreationDate || container.getImportDate().equals(filterByContainer.getImportDate()))
                                    && (!filterByDeleted || container.getDeleted().equalsIgnoreCase(filterByContainer.getDeleted()))
                                    && (!filterByIsSystem || container.getSystem().equalsIgnoreCase(filterByContainer.getSystem()))
                                    ).collect(Collectors.toList());
                        }

                        if(!containersByCurrentUser.isEmpty()){
                            sortUtils.sortItems("des", containersByCurrentUser, Comparator.comparing(Container::getName, Comparator.nullsLast(Comparator.naturalOrder())));

                            int totalRecords = containersByCurrentUser.size();

                            int endIndex;
                            if(limit != -1){
                                 endIndex = Math.min((startIndex + limit), totalRecords);
                            }
                            else{
                                endIndex = totalRecords;
                            }


                            List<Container> containersOnPage = containersByCurrentUser.subList(startIndex, endIndex);

                            List<ContainerUser> containerUsersByUserId = containerUsersRepository.findContainerUsersByUserId(user.getId());

                            if(!containerUsersByUserId.isEmpty()){
                                for(Container containerOnPage: containersOnPage){
                                    for(ContainerUser containerUser: containerUsersByUserId){
                                        if(containerOnPage.getId().equalsIgnoreCase(containerUser.getContainerId())){
                                            containerOnPage.setSubscribed(ToyboxConstants.LOOKUP_YES);
                                            break;
                                        }
                                        containerOnPage.setSubscribed(ToyboxConstants.LOOKUP_NO);
                                    }
                                }
                            }
                            else{
                                for(Container containerOnPage: containersOnPage){
                                    containerOnPage.setSubscribed(ToyboxConstants.LOOKUP_NO);
                                }
                            }

                            retrieveContainersResults.setBreadcrumbs(containerUtils.generateContainerPath(null));

                            retrieveContainersResults.setTotalRecords(totalRecords);
                            retrieveContainersResults.setContainers(containersOnPage);

                            retrieveContainersResults.setMessage("Folders retrieved successfully!");
                            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.OK);
                        }
                        else{
                            String message = "There are no containers to return.";
                            _logger.debug(message);

                            retrieveContainersResults.setMessage(message);

                            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.OK);
                        }
                    }
                    else{
                        throw new IllegalArgumentException("User is null!");
                    }
                }
                else{
                    String errorMessage = "Container search request is null!";
                    _logger.debug(errorMessage);

                    retrieveContainersResults.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveContainersResults, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveContainersResults.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveContainersResults, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving containers. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveContainersResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}