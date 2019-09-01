package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.container.*;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.utilities.*;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private AssetUtils assetUtils;
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
    private ContainersRepository containersRepository;
    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;
    @Autowired
    private ContainerUsersRepository containerUsersRepository;
    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private AssetUserRepository assetUserRepository;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/containers", method = RequestMethod.POST)
    public ResponseEntity<CreateContainerResponse> createContainer(Authentication authentication, @RequestBody CreateContainerRequest createContainerRequest){
        CreateContainerResponse createContainerResponse = new CreateContainerResponse();

        try {
            if(authenticationUtils.isSessionValid(authentication)){
                if(createContainerRequest != null){
                    if(StringUtils.isNotBlank(createContainerRequest.getContainerName())){
                        boolean canCreateFolder;
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
                            Container parentContainer = containerUtils.getContainer(createContainerRequest.getParentContainerId());
                            Container duplicateContainer = containerUtils.findDuplicateContainer(createContainerRequest.getParentContainerId(), createContainerRequest.getContainerName());
                            if(duplicateContainer == null){
                                canCreateFolder = true;
                            }
                            else{
                                canCreateFolder = false;
                                errorMessage = "The folder '" + parentContainer.getName() + "' already has a folder named '" + createContainerRequest.getContainerName() + "'.";
                            }
                        }

                        if(canCreateFolder){
                            Container container = new Container();
                            container.setId(generateFolderId());
                            container.setName(createContainerRequest.getContainerName());
                            container.setParentId(createContainerRequest.getParentContainerId());
                            container.setCreatedByUsername(authentication.getName());
                            container.setCreationDate(Calendar.getInstance().getTime());
                            container.setDeleted("N");
                            container.setSystem("N");

                            createContainer(container);

                            createContainerResponse.setContainerId(container.getId());
                            createContainerResponse.setMessage("Folder created successfully!");
                            return new ResponseEntity<>(createContainerResponse, HttpStatus.OK);
                        }
                        else{
                            _logger.error(errorMessage);

                            createContainerResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(createContainerResponse, HttpStatus.BAD_REQUEST);
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
                            String sortColumn = assetSearchRequest.getSortColumn();
                            String sortType = assetSearchRequest.getSortType();
                            int startIndex = assetSearchRequest.getOffset();
                            int limit = assetSearchRequest.getLimit();
                            List<SearchRequestFacet> searchRequestFacetList = assetSearchRequest.getAssetSearchRequestFacetList();

                            List<Container> containersByCurrentUser;
                            List<Asset> assetsByCurrentUser = new ArrayList<>();

                            List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(container.getId());
                            List<String> containerAssetIdsByContainerId = containerAssetsByContainerId.stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());

                            List<Asset> allAssets;
                            if(!containerAssetIdsByContainerId.isEmpty()){
                                allAssets = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(containerAssetIdsByContainerId);
                            }
                            else{
                                allAssets = new ArrayList<>();
                            }

                            List<Asset> assets;
                            if(searchRequestFacetList != null && !searchRequestFacetList.isEmpty()){
                                assets = allAssets.stream().filter(asset -> facetUtils.hasFacetValue(asset, searchRequestFacetList)).collect(Collectors.toList());
                            }
                            else{
                                assets = allAssets;
                            }

                            if(authenticationUtils.isAdminUser(authentication)){
                                _logger.debug("Retrieving all the items in the container [Admin User]...");
                                containersByCurrentUser = containersRepository.getNonDeletedContainersByParentContainerId(container.getId());
                                assetsByCurrentUser = assets.stream()
                                        .filter(asset -> asset.getIsLatestVersion().equalsIgnoreCase("Y"))
                                        .collect(Collectors.toList());
                            }
                            else{
                                _logger.debug("Retrieving the items of the user '" + user.getUsername() + "'...");
                                containersByCurrentUser = containersRepository.getNonDeletedContainersByUsernameAndParentContainerId(user.getUsername(), container.getId());

                                for(Asset asset: assets){
                                    if(StringUtils.isNotBlank(asset.getImportedByUsername()) && asset.getIsLatestVersion().equalsIgnoreCase("Y")){
                                        if(asset.getImportedByUsername().equalsIgnoreCase(user.getUsername())){
                                            assetsByCurrentUser.add(asset);
                                        }
                                        else{
                                            Asset originalAsset= assetUtils.getAsset(asset.getOriginalAssetId());
                                            if(originalAsset.getImportedByUsername().equalsIgnoreCase(user.getUsername())){
                                                assetsByCurrentUser.add(asset);
                                            }
                                        }
                                    }
                                }
                            }

                            // Set facets
                            List<Facet> facets = facetUtils.getFacets(assetsByCurrentUser);
                            retrieveContainerContentsResult.setFacets(facets);

                            // Sort containers
                            sortUtils.sortItems("asc", containersByCurrentUser, Comparator.comparing(Container::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            // Sort assets
                            if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
                                sortUtils.sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                                sortUtils.sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            }

                            // Merge containers and assets
                            List<ContainerItem> containerItems = new ArrayList<>(containersByCurrentUser);
                            containerItems.addAll(assetsByCurrentUser);

                            // Paginate results
                            int totalRecords = containerItems.size();
                            int endIndex = Math.min((startIndex + limit), totalRecords);

                            List<ContainerItem> containerItemsOnPage = containerItems.subList(startIndex, endIndex);

                            // Set subscription status
                            List<ContainerUser> containerUsersByUserId = containerUsersRepository.findContainerUsersByUserId(user.getId());
                            List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());

                            for(ContainerItem containerItem: containerItemsOnPage){
                                Class<? extends ContainerItem> containerItemClass = containerItem.getClass();

                                if(containerItemClass.getName().equalsIgnoreCase("com.github.murataykanat.toybox.dbo.Container")){
                                    Container containerOnPage = (Container) containerItem;

                                    containerOnPage.setSubscribed("N");
                                    for(ContainerUser containerUser: containerUsersByUserId){
                                        if(containerOnPage.getId().equalsIgnoreCase(containerUser.getContainerId())){
                                            containerOnPage.setSubscribed("Y");
                                            break;
                                        }
                                    }
                                }
                                else if(containerItemClass.getName().equalsIgnoreCase("com.github.murataykanat.toybox.dbo.Asset")){
                                    Asset assetOnPage = (Asset) containerItem;

                                    assetOnPage.setSubscribed("N");
                                    for(AssetUser assetUser: assetUsersByUserId){
                                        if(assetOnPage.getId().equalsIgnoreCase(assetUser.getAssetId())){
                                            assetOnPage.setSubscribed("Y");
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
                                            throw new Exception("Asset is in multiple folders!");
                                        }
                                    }
                                    else{
                                        throw new Exception("Asset is not in any folder!");
                                    }
                                }
                            }

                            // Set breadcrumbs
                            retrieveContainerContentsResult.setBreadcrumbs(generateContainerPath(container.getId()));

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
                            Container oldContainer = containerUtils.getContainer(containerId);
                            Container container = containerUtils.updateContainer(updateContainerRequest, containerId);
                            if(container != null){
                                // Send notification
                                String notification = "Folder '" + oldContainer.getName() + "' is updated by '" + user.getUsername() + "'";
                                SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                sendNotificationRequest.setIsAsset(false);
                                sendNotificationRequest.setId(oldContainer.getId());
                                sendNotificationRequest.setFromUser(user);
                                sendNotificationRequest.setMessage(notification);
                                notificationUtils.sendNotification(sendNotificationRequest, session);

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

                        if(containerSearchRequest.getRetrieveTopLevelContainers() != null && containerSearchRequest.getRetrieveTopLevelContainers().equalsIgnoreCase("Y")){
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
                            containersByCurrentUser = containersRepository.getUserFolders(user.getUsername());
                            Container filterByContainer = containerSearchRequest.getContainer();
                            boolean filterById = StringUtils.isNotBlank(filterByContainer.getId());
                            boolean filterByParentId = StringUtils.isNotBlank(filterByContainer.getParentId());
                            boolean filterByName = StringUtils.isNotBlank(filterByContainer.getName());
                            boolean filterByCreatedByUsername = StringUtils.isNotBlank(filterByContainer.getCreatedByUsername());
                            boolean filterByCreationDate = filterByContainer.getCreationDate() != null;
                            boolean filterByDeleted = StringUtils.isNotBlank(filterByContainer.getDeleted());
                            boolean filterByIsSystem = StringUtils.isNotBlank(filterByContainer.getSystem());

                            containersByCurrentUser = containersByCurrentUser.stream()
                                    .filter(container ->
                                    (!filterById || container.getId().contains(filterByContainer.getId()))
                                    && (!filterByParentId || container.getParentId().contains(filterByContainer.getParentId()))
                                    && (!filterByName || container.getName().contains(filterByContainer.getName()))
                                    && (!filterByCreatedByUsername || container.getCreatedByUsername().contains(filterByContainer.getCreatedByUsername()))
                                    && (!filterByCreationDate || container.getCreationDate().equals(filterByContainer.getCreationDate()))
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
                                            containerOnPage.setSubscribed("Y");
                                            break;
                                        }
                                        containerOnPage.setSubscribed("N");
                                    }
                                }
                            }
                            else{
                                for(Container containerOnPage: containersOnPage){
                                    containerOnPage.setSubscribed("N");
                                }
                            }

                            retrieveContainersResults.setBreadcrumbs(generateContainerPath(null));

                            retrieveContainersResults.setTotalRecords(totalRecords);
                            retrieveContainersResults.setContainers(containersOnPage);

                            retrieveContainersResults.setMessage("Assets retrieved successfully!");
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

    @LogEntryExitExecutionTime
    private String generateFolderId(){
        String containerId = RandomStringUtils.randomAlphanumeric(ToyboxConstants.FOLDER_ID_LENGTH);
        if(isContainerIdValid(containerId)){
            return containerId;
        }
        return generateFolderId();
    }

    @LogEntryExitExecutionTime
    private boolean isContainerIdValid(String containerId){
        List<Container> containers = containersRepository.getContainersById(containerId);
        List<Asset> assets = assetsRepository.getAssetsById(containerId);
        if(containers.isEmpty() && assets.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }

    @LogEntryExitExecutionTime
    private void createContainer(Container container){
        _logger.debug("Container ID: " + container.getId());
        _logger.debug("Container name: " + container.getName());
        _logger.debug("Container parent ID: " + container.getParentId());
        _logger.debug("Container created by username: " + container.getCreatedByUsername());
        _logger.debug("Container creation date: " + container.getCreationDate());

        containersRepository.insertContainer(container.getId(), container.getName(), container.getParentId(),
                container.getCreatedByUsername(), container.getCreationDate(), container.getDeleted(),
                container.getSystem());
    }

    @LogEntryExitExecutionTime
    private List<Breadcrumb> generateContainerPath(String containerId) throws Exception {
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        Container container;
        Breadcrumb breadcrumb = new Breadcrumb();

        if(StringUtils.isNotBlank(containerId) && !containerId.equalsIgnoreCase("null")){
            container = containerUtils.getContainer(containerId);
            breadcrumb.setContainerId(container.getId());
            breadcrumb.setContainerName(container.getName());
            breadcrumbs.add(breadcrumb);

            while (StringUtils.isNotBlank(container.getParentId())){
                container = containerUtils.getContainer(container.getParentId());

                breadcrumb = new Breadcrumb();
                breadcrumb.setContainerId(container.getId());
                breadcrumb.setContainerName(container.getName());

                breadcrumbs.add(breadcrumb);
            }

            breadcrumb = new Breadcrumb();
            breadcrumb.setContainerId(null);
            breadcrumb.setContainerName("Root");

            breadcrumbs.add(breadcrumb);
        }
        else{
            breadcrumb = new Breadcrumb();
            breadcrumb.setContainerId(null);
            breadcrumb.setContainerName("Root");

            breadcrumbs.add(breadcrumb);
        }

        return breadcrumbs;
    }
}
