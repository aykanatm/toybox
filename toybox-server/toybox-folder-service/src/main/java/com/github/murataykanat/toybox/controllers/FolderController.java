package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.container.*;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import com.github.murataykanat.toybox.utilities.SortUtils;
import com.github.murataykanat.toybox.utils.Constants;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class FolderController {
    private static final Log _logger = LogFactory.getLog(FolderController.class);

    private static final String assetServiceLoadBalancerServiceName = "toybox-asset-loadbalancer";
    private static final String notificationServiceLoadBalancerServiceName = "toybox-notification-loadbalancer";

    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private UsersRepository usersRepository;
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

    @RequestMapping(value = "/containers", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> createContainer(Authentication authentication, @RequestBody CreateContainerRequest createContainerRequest){
        _logger.debug("createContainer() >>");
        GenericResponse genericResponse = new GenericResponse();
        try {
            if(isSessionValid(authentication)){
                if(createContainerRequest != null){
                    if(StringUtils.isNotBlank(createContainerRequest.getContainerName())){
                        boolean canCreateFolder = false;
                        if(StringUtils.isBlank(createContainerRequest.getParentContainerId())){
                            if(isAdminUser(authentication)){
                                canCreateFolder = true;
                            }
                            else{
                                canCreateFolder = false;
                            }
                        }
                        else{
                            canCreateFolder = true;
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

                            genericResponse.setMessage("Folder created successfully!");
                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            String errorMessage;
                            if(StringUtils.isBlank(createContainerRequest.getParentContainerId())){
                                errorMessage = "The user '" + authentication.getName() + "' is not allowed to create a folder under the root!";
                            }
                            else{
                                errorMessage = "The user '" + authentication.getName() + "' is not allowed to create a folder under the folder with ID '" + createContainerRequest.getParentContainerId() + "'";
                            }

                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            _logger.debug("<< createContainer()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Container name is blank!";
                        _logger.debug(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< createContainer()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Create container request is null!";
                    _logger.debug(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< createContainer()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< createContainer()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while creating the container. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< createContainer()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/containers/{containerId}/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainerContentsResult> retrieveContainerContents(Authentication authentication, @PathVariable String containerId, @RequestBody AssetSearchRequest assetSearchRequest){
        _logger.debug("retrieveContainerContents() >>");
        RetrieveContainerContentsResult retrieveContainerContentsResult = new RetrieveContainerContentsResult();
        try{
            if(isSessionValid(authentication)){
                if(StringUtils.isNotBlank(containerId)){
                    if(assetSearchRequest != null){
                        Container container = getContainer(containerId);
                        User user = getUser(authentication);
                        if(user != null){
                            String sortColumn = assetSearchRequest.getSortColumn();
                            String sortType = assetSearchRequest.getSortType();
                            int offset = assetSearchRequest.getOffset();
                            int limit = assetSearchRequest.getLimit();
                            List<SearchRequestFacet> searchRequestFacetList = assetSearchRequest.getAssetSearchRequestFacetList();

                            List<Container> containersByCurrentUser;
                            List<Asset> assetsByCurrentUser;

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
                                assets = allAssets.stream().filter(asset -> FacetUtils.getInstance().hasFacetValue(asset, searchRequestFacetList)).collect(Collectors.toList());
                            }
                            else{
                                assets = allAssets;
                            }

                            if(isAdminUser(authentication)){
                                _logger.debug("Retrieving all the items in the container [Admin User]...");
                                containersByCurrentUser = containersRepository.getNonDeletedContainersByParentContainerId(container.getId());
                                assetsByCurrentUser = assets.stream()
                                        .filter(asset -> asset.getIsLatestVersion().equalsIgnoreCase("Y"))
                                        .collect(Collectors.toList());
                            }
                            else{
                                _logger.debug("Retrieving the items of the user '" + user.getUsername() + "'...");
                                containersByCurrentUser = containersRepository.getNonDeletedContainersByUsernameAndParentContainerId(user.getUsername(), container.getId());
                                assetsByCurrentUser = assets.stream()
                                        .filter(asset -> asset.getImportedByUsername() != null && asset.getImportedByUsername().equalsIgnoreCase(user.getUsername()) && asset.getIsLatestVersion().equalsIgnoreCase("Y"))
                                        .collect(Collectors.toList());
                            }

                            // Set facets
                            List<Facet> facets = FacetUtils.getInstance().getFacets(assetsByCurrentUser);
                            retrieveContainerContentsResult.setFacets(facets);

                            // Sort containers
                            SortUtils.getInstance().sortItems("asc", containersByCurrentUser, Comparator.comparing(Container::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            // Sort assets
                            if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
                                SortUtils.getInstance().sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                                SortUtils.getInstance().sortItems(sortType, assetsByCurrentUser, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            }

                            // Merge containers and assets
                            List<ContainerItem> containerItems = new ArrayList<>(containersByCurrentUser);
                            containerItems.addAll(assetsByCurrentUser);

                            // Paginate results
                            int totalRecords = containerItems.size();
                            int startIndex = offset;
                            int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                            List<ContainerItem> containerItemsOnPage = containerItems.subList(startIndex, endIndex);

                            // Set subscription status
                            List<ContainerUser> containerUsersByUserId = containerUsersRepository.findContainerUsersByUserId(user.getId());
                            List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());

                            for(ContainerItem containerItem: containerItemsOnPage){
                                if(containerItem.getClass().isInstance(Container.class)){
                                    Container containerOnPage = (Container) containerItem;

                                    containerOnPage.setSubscribed("N");
                                    for(ContainerUser containerUser: containerUsersByUserId){
                                        if(containerOnPage.getId().equalsIgnoreCase(containerUser.getContainerId())){
                                            containerOnPage.setSubscribed("Y");
                                            break;
                                        }
                                    }
                                }
                                else if(containerItem.getClass().isInstance(Asset.class)){
                                    Asset assetOnPage = (Asset) containerItem;

                                    assetOnPage.setSubscribed("N");
                                    for(AssetUser assetUser: assetUsersByUserId){
                                        if(assetOnPage.getId().equalsIgnoreCase(assetUser.getAssetId())){
                                            assetOnPage.setSubscribed("Y");
                                            break;
                                        }
                                    }
                                }
                            }

                            // Set breadcrumbs
                            retrieveContainerContentsResult.setBreadcrumbs(generateContainerPath(container.getId()));

                            // Finalize
                            retrieveContainerContentsResult.setTotalRecords(totalRecords);
                            retrieveContainerContentsResult.setContainerItems(containerItemsOnPage);
                            retrieveContainerContentsResult.setMessage("Container contents retrieved successfully!");

                            _logger.debug("<< retrieveContainerContents()");
                            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.OK);
                        }
                        else{
                            throw new Exception("User is null!");
                        }
                    }
                    else{
                        String errorMessage = "Asset search request is null!";
                        _logger.debug(errorMessage);

                        retrieveContainerContentsResult.setMessage(errorMessage);

                        _logger.debug("<< retrieveContainerContents()");
                        return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Container ID is blank!";
                    _logger.debug(errorMessage);

                    retrieveContainerContentsResult.setMessage(errorMessage);

                    _logger.debug("<< retrieveContainerContents()");
                    return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveContainerContentsResult.setMessage(errorMessage);

                _logger.debug("<< retrieveContainerContents()");
                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving items inside the container with ID '" + containerId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveContainerContentsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveContainerContents()");
            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/containers/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainersResults> retrieveContainers(Authentication authentication, @RequestBody ContainerSearchRequest containerSearchRequest){
        _logger.debug("retrieveContainers() >>");
        RetrieveContainersResults retrieveContainersResults = new RetrieveContainersResults();
        try {
            if(isSessionValid(authentication)){
                if(containerSearchRequest != null){
                    User user = getUser(authentication);
                    if(user != null){
                        int offset = containerSearchRequest.getOffset();
                        int limit = containerSearchRequest.getLimit();

                        List<Container> containersByCurrentUser;

                        if(containerSearchRequest.getRetrieveTopLevelContainers() != null && containerSearchRequest.getRetrieveTopLevelContainers().equalsIgnoreCase("Y")){
                            if(isAdminUser(authentication)){
                                _logger.debug("Retrieving the top level containers [Admin User]...");
                                containersByCurrentUser = containersRepository.getTopLevelNonDeletedContainers();
                            }
                            else{
                                String errorMessage = "The user '" + user.getUsername() + "' does not have permissions to retrieve the top level containers!";
                                _logger.error(errorMessage);

                                retrieveContainersResults.setMessage(errorMessage);

                                _logger.debug("<< retrieveContainers()");
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
                            SortUtils.getInstance().sortItems("des", containersByCurrentUser, Comparator.comparing(Container::getName, Comparator.nullsLast(Comparator.naturalOrder())));

                            int totalRecords = containersByCurrentUser.size();
                            int startIndex = offset;
                            int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

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

                            _logger.debug("<< retrieveContainers()");
                            retrieveContainersResults.setMessage("Assets retrieved successfully!");
                            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.OK);
                        }
                        else{
                            String message = "There are no containers to return.";
                            _logger.debug(message);

                            retrieveContainersResults.setMessage(message);

                            _logger.debug("<< retrieveContainers()");
                            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.OK);
                        }
                    }
                    else{
                        throw new Exception("User is null!");
                    }
                }
                else{
                    String errorMessage = "Container search request is null!";
                    _logger.debug(errorMessage);

                    retrieveContainersResults.setMessage(errorMessage);

                    _logger.debug("<< retrieveContainers()");
                    return new ResponseEntity<>(retrieveContainersResults, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveContainersResults.setMessage(errorMessage);

                _logger.debug("<< retrieveContainers()");
                return new ResponseEntity<>(retrieveContainersResults, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving containers. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveContainersResults.setMessage(errorMessage);

            _logger.debug("<< retrieveContainers()");
            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getLoadbalancerUrl(String loadbalancerServiceName) throws Exception {
        _logger.debug("getLoadbalancerUrl() [" + loadbalancerServiceName + "]");
        List<ServiceInstance> instances = discoveryClient.getInstances(loadbalancerServiceName);
        if(!instances.isEmpty()){
            ServiceInstance serviceInstance = instances.get(0);
            _logger.debug("Load balancer URL: " + serviceInstance.getUri().toString());
            _logger.debug("<< getLoadbalancerUrl()");
            return serviceInstance.getUri().toString();
        }
        else{
            throw new Exception("There is no load balancer instance with name '" + loadbalancerServiceName + "'.");
        }
    }

    private void sendNotification(SendNotificationRequest sendNotificationRequest, HttpSession session) throws Exception {
        _logger.debug("sendNotification() >>");
        HttpHeaders headers = getHeaders(session);
        String loadbalancerUrl = getLoadbalancerUrl(notificationServiceLoadBalancerServiceName);
        HttpEntity<SendNotificationRequest> sendNotificationRequestHttpEntity = new HttpEntity<>(sendNotificationRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<GenericResponse> genericResponseResponseEntity = restTemplate.postForEntity(loadbalancerUrl + "/notifications", sendNotificationRequestHttpEntity, GenericResponse.class);

        boolean successful = genericResponseResponseEntity.getStatusCode().is2xxSuccessful();

        if(successful){
            _logger.debug("Notification was send successfully!");
            _logger.debug("<< sendNotification()");
        }
        else{
            throw new Exception("An error occurred while sending a notification. " + genericResponseResponseEntity.getBody().getMessage());
        }
    }

    private HttpHeaders getHeaders(HttpSession session) throws Exception {
        _logger.debug("getHeaders() >>");
        HttpHeaders headers = new HttpHeaders();

        _logger.debug("Session ID: " + session.getId());
        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
        if(token != null){
            _logger.debug("CSRF Token: " + token.getToken());
            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
            headers.set("X-XSRF-TOKEN", token.getToken());

            _logger.debug("<< getHeaders()");
            return headers;
        }
        else{
            throw new Exception("CSRF token is null!");
        }
    }

    private boolean isSessionValid(Authentication authentication){
        _logger.debug("isSessionValid() >>");
        String errorMessage;
        List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
                _logger.debug("<< isSessionValid() [true]");
                return true;
            }
            else{
                errorMessage = "Username '" + authentication.getName() + "' is not unique!";
            }
        }
        else{
            errorMessage = "No users with username '" + authentication.getName() + " is found!";
        }

        _logger.error(errorMessage);
        _logger.debug("<< isSessionValid() [false]");
        return false;
    }

    private User getUser(Authentication authentication){
        _logger.debug("getUser() >>");
        String errorMessage;
        List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
                _logger.debug("<< getUser()");
                return usersByUsername.get(0);
            }
            else{
                errorMessage = "Username '" + authentication.getName() + "' is not unique!";
            }
        }
        else{
            errorMessage = "No users with username '" + authentication.getName() + " is found!";
        }

        _logger.error(errorMessage);
        _logger.debug("<< getUser()");
        return null;
    }

    private boolean isAdminUser(Authentication authentication){
        _logger.debug("isAdminUser() >>");
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<? extends GrantedAuthority> roleAdmin = authorities.stream().filter(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")).collect(Collectors.toList());
        if(!roleAdmin.isEmpty()){
            _logger.debug("<< isAdminUser() [false]");
            return true;
        }

        _logger.debug("<< isAdminUser() [false]");
        return false;
    }

    private String generateFolderId(){
        _logger.debug("generateFolderId() >>");
        String containerId = RandomStringUtils.randomAlphanumeric(Constants.FOLDER_ID_LENGTH);
        if(isContainerIdValid(containerId)){
            _logger.debug("<< generateFolderId() [" + containerId + "]");
            return containerId;
        }
        return generateFolderId();
    }

    private boolean isContainerIdValid(String containerId){
        _logger.debug("isContainerIdValid() >> [" + containerId + "]");
        boolean result = false;

        List<Container> containers = containersRepository.getContainersById(containerId);
        if(containers != null){
            if(!containers.isEmpty()){
                _logger.debug("<< isContainerIdValid() [" + false + "]");
                result = false;
            }
            else{
                _logger.debug("<< isContainerIdValid() [" + true + "]");
                result = true;
            }
        }

        _logger.debug("<< isContainerIdValid() [" + true + "]");
        return result;
    }

    private void createContainer(Container container){
        _logger.debug("createContainer() >>");

        _logger.debug("Container ID: " + container.getId());
        _logger.debug("Container name: " + container.getName());
        _logger.debug("Container parent ID: " + container.getParentId());
        _logger.debug("Container created by username: " + container.getCreatedByUsername());
        _logger.debug("Container creation date: " + container.getCreationDate());

        containersRepository.insertContainer(container.getId(), container.getName(), container.getParentId(),
                container.getCreatedByUsername(), container.getCreationDate(), container.getDeleted(),
                container.getSystem());

        _logger.debug("<< createContainer()");
    }

    private List<Breadcrumb> generateContainerPath(String containerId) throws Exception {
        _logger.debug("generateContainerPath() >>");
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        Container container;
        Breadcrumb breadcrumb = new Breadcrumb();

        if(StringUtils.isNotBlank(containerId) && !containerId.equalsIgnoreCase("null")){
            container = getContainer(containerId);
            breadcrumb.setContainerId(container.getId());
            breadcrumb.setContainerName(container.getName());
            breadcrumbs.add(breadcrumb);

            while (StringUtils.isNotBlank(container.getParentId())){
                container = getContainer(container.getParentId());

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

        _logger.debug("<< generateContainerPath()");
        return breadcrumbs;
    }

    private Container getContainer(String containerId) throws Exception {
        List<Container> containersById = containersRepository.getContainersById(containerId);
        if(!containersById.isEmpty()){
            if(containersById.size() == 1){
                return containersById.get(0);
            }
            else{
                throw new Exception("There are multiple containers with ID '" + containerId + "'!");
            }
        }
        else{
            throw new Exception("There is no container with ID '" + containerId + "'!");
        }
    }
}
