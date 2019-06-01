package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.AssetUser;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.*;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobResult;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import com.github.murataykanat.toybox.utilities.SortUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class AssetController {
    private static final Log _logger = LogFactory.getLog(AssetController.class);

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

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @RequestMapping(value = "/assets/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("downloadAssets() >>");

        try{
            if(selectedAssets != null){
                if(isSessionValid(authentication)){
                    List<Asset> assets = selectedAssets.getSelectedAssets();
                    if(assets != null){
                        if(!assets.isEmpty()){
                            if(assets.size() == 1) {
                                _logger.debug("Downloading a single asset...");
                                List<Asset> assetsWithId = assetsRepository.getAssetsById(assets.get(0).getId());
                                if(assetsWithId != null){
                                    if(assetsWithId.size() == 1){
                                        Asset asset = assetsWithId.get(0);
                                        if(asset != null){
                                            if(StringUtils.isNotBlank(asset.getPath())){
                                                File file = new File(asset.getPath());
                                                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                                                _logger.debug("<< downloadAssets()");
                                                return new ResponseEntity<>(resource, HttpStatus.OK);
                                            }
                                            else{
                                                _logger.error("Asset path is blank!");
                                                _logger.debug("<< downloadAssets()");
                                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                                            }
                                        }
                                        else{
                                            throw new InvalidObjectException("Asset is null!");
                                        }
                                    }
                                    else{
                                        throw new DuplicateKeyException("There are more than one asset with ID '" + assets.get(0).getId() + "'");
                                    }
                                }
                                else{
                                    _logger.warn("Asset with ID '" + assets.get(0).getId() + "' is not found!");
                                    _logger.debug("<< downloadAssets()");
                                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                                }
                            }
                            else{
                                _logger.debug("Downloading multiple assets...");
                                RestTemplate restTemplate = new RestTemplate();
                                HttpHeaders headers = getHeaders(session);

                                HttpEntity<SelectedAssets> selectedAssetsEntity = new HttpEntity<>(selectedAssets, headers);

                                List<ServiceInstance> instances = discoveryClient.getInstances(jobServiceLoadBalancerServiceName);
                                if(!instances.isEmpty()){
                                    ServiceInstance serviceInstance = instances.get(0);
                                    String jobServiceUrl = serviceInstance.getUri().toString();
                                    ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/package", selectedAssetsEntity, JobResponse.class);
                                    if(jobResponseResponseEntity != null){
                                        _logger.debug(jobResponseResponseEntity);
                                        JobResponse jobResponse = jobResponseResponseEntity.getBody();
                                        if(jobResponse != null){
                                            _logger.debug("Job response message: " + jobResponse.getMessage());
                                            _logger.debug("Job ID: " + jobResponse.getJobId());
                                            File archiveFile = getArchiveFile(jobResponse.getJobId(), headers, jobServiceUrl);
                                            if(archiveFile != null && archiveFile.exists()){
                                                InputStreamResource resource = new InputStreamResource(new FileInputStream(archiveFile));

                                                _logger.debug("<< downloadAssets()");
                                                return new ResponseEntity<>(resource, HttpStatus.OK);
                                            }
                                            else{
                                                if(archiveFile != null){
                                                    throw new IOException("File '" + archiveFile.getAbsolutePath() + "' does not exist!");
                                                }
                                                throw new InvalidObjectException("Archive file is null!");
                                            }
                                        }
                                        else{
                                            throw new InvalidObjectException("Job response is null!");
                                        }
                                    }
                                    else{
                                        throw new InvalidObjectException("Job response entity is null!");
                                    }
                                }
                                else{
                                    throw new Exception("There is no job load balancer instance!");
                                }
                            }
                        }
                        else{
                            _logger.debug("<< downloadAssets()");
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }
                    }
                    else{
                        throw new InvalidObjectException("Asset list is null!");
                    }
                }
                else{
                    String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                    _logger.error(errorMessage);

                    _logger.debug("<< downloadAssets()");
                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Selected assets paramater is null!";
                _logger.error(errorMessage);

                _logger.debug("<< downloadAssets()");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading selected assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< downloadAssets()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/upload", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> uploadAssets(Authentication authentication, HttpSession session, @RequestBody UploadFileLst uploadFileLst) {
        _logger.debug("uploadAssets() >>");
        try{
            GenericResponse genericResponse = new GenericResponse();

            if(uploadFileLst != null){
                if(isSessionValid(authentication)){
                    RestTemplate restTemplate = new RestTemplate();

                    HttpHeaders headers = getHeaders(session);
                    HttpEntity<UploadFileLst> selectedAssetsEntity = new HttpEntity<>(uploadFileLst, headers);

                    List<ServiceInstance> instances = discoveryClient.getInstances(jobServiceLoadBalancerServiceName);
                    if(!instances.isEmpty()){
                        ServiceInstance serviceInstance = instances.get(0);
                        String jobServiceUrl = serviceInstance.getUri().toString();
                        ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/import", selectedAssetsEntity, JobResponse.class);
                        boolean successful = jobResponseResponseEntity.getStatusCode().is2xxSuccessful();

                        if(successful){
                            genericResponse.setMessage(uploadFileLst.getUploadFiles().size() + " file(s) successfully uploaded. Import job started.");
                            _logger.debug("<< uploadAssets()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.CREATED);
                        }
                        else{
                            genericResponse.setMessage("Upload was successful but import failed to start. " + jobResponseResponseEntity.getBody().getMessage());
                            _logger.debug("<< uploadAssets()");
                            return new ResponseEntity<>(genericResponse, jobResponseResponseEntity.getStatusCode());
                        }
                    }
                    else{
                        throw new Exception("There is no job load balancer instance!");
                    }
                }
                else{
                    String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< updateNotifications()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Upload file list parameter is null!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< updateNotifications()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while starting the import job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< uploadAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveAssetsResults> retrieveAssets(Authentication authentication, @RequestBody AssetSearchRequest assetSearchRequest){
        _logger.debug("retrieveAssets() >>");
        try{
            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            if(assetSearchRequest != null){
                if(isSessionValid(authentication)){
                    List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                    if(!usersByUsername.isEmpty()){
                        if(usersByUsername.size() == 1){
                            User user = usersByUsername.get(0);

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

                                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

                                // TODO: Change the logic to search folders service
                                List<Asset> assetsByCurrentUser;
                                List<? extends GrantedAuthority> role_admin = authorities.stream().filter(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")).collect(Collectors.toList());
                                if(!role_admin.isEmpty()){
                                    _logger.debug("Retrieving all assets [Admin User]...");
                                    assetsByCurrentUser = assets.stream()
                                            .filter(asset -> asset.getIsLatestVersion().equalsIgnoreCase("Y"))
                                            .collect(Collectors.toList());
                                }
                                else{
                                    _logger.debug("Retrieving assets of the user '" + user.getUsername() + "'...");
                                    assetsByCurrentUser = assets.stream()
                                            .filter(asset -> asset.getImportedByUsername() != null && asset.getImportedByUsername().equalsIgnoreCase(user.getUsername()) && asset.getIsLatestVersion().equalsIgnoreCase("Y"))
                                            .collect(Collectors.toList());
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
                                // TODO: Change the logic to search folders service
                                for(Asset assetOnPage: assetsOnPage){
                                    if(!assetUsersByUserId.isEmpty()){
                                        for(AssetUser assetUser: assetUsersByUserId){
                                            if(assetOnPage.getId().equalsIgnoreCase(assetUser.getAssetId())){
                                                assetOnPage.setSubscribed("Y");
                                                break;
                                            }
                                            assetOnPage.setSubscribed("N");
                                        }
                                    }
                                    else{
                                        assetOnPage.setSubscribed("N");
                                    }
                                }

                                retrieveAssetsResults.setTotalRecords(totalRecords);
                                retrieveAssetsResults.setAssets(assetsOnPage);

                                _logger.debug("<< retrieveAssets()");
                                retrieveAssetsResults.setMessage("Assets retrieved successfully!");
                                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.OK);
                            }
                            else{
                                String message = "There are no assets to return.";
                                _logger.debug(message);

                                retrieveAssetsResults.setMessage(message);

                                _logger.debug("<< retrieveAssets()");
                                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.NO_CONTENT);
                            }
                        }
                        else{
                            throw new Exception("Username '" + authentication.getName() + "' is not unique!");
                        }
                    }
                    else{
                        throw new Exception("No users with username '" + authentication.getName() + " is found!");
                    }
                }
                else{
                    String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                    _logger.error(errorMessage);

                    retrieveAssetsResults.setMessage(errorMessage);

                    _logger.debug("<< retrieveAssets()");
                    return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Asset search request is null!";
                _logger.debug(errorMessage);

                retrieveAssetsResults.setMessage(errorMessage);

                _logger.debug("<< retrieveAssets()");
                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            _logger.debug("<< retrieveAssets()");
            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> deleteAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssets() >>");
        try{
            GenericResponse genericResponse = new GenericResponse();

            if(selectedAssets != null){
                if(isSessionValid(authentication)){
                    User user = getUser(authentication);
                    if(user != null){
                        if(!selectedAssets.getSelectedAssets().isEmpty()){
                            List<Asset> assetsAndVersions = new ArrayList<>();

                            for(Asset selectedAsset: selectedAssets.getSelectedAssets()){
                                List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                assetsAndVersions.addAll(assetsByOriginalAssetId);
                            }

                            if(!assetsAndVersions.isEmpty()){
                                assetsRepository.deleteAssetById("Y",assetsAndVersions.stream().map(a -> a.getId()).collect(Collectors.toList()));

                                for(Asset asset: selectedAssets.getSelectedAssets()){
                                    // Send notification
                                    String message = "Asset '" + asset.getName() + "' is deleted by '" + user.getUsername() + "'";
                                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                    sendNotificationRequest.setAsset(asset);
                                    sendNotificationRequest.setFromUser(user);
                                    sendNotificationRequest.setMessage(message);
                                    sendNotification(sendNotificationRequest, session);
                                }

                                for(Asset asset: assetsAndVersions){
                                    List<User> usersByUsername = usersRepository.findUsersByUsername(asset.getImportedByUsername());
                                    if(!usersByUsername.isEmpty()){
                                        if(usersByUsername.size() == 1){
                                            User importUser = usersByUsername.get(0);
                                            assetUserRepository.deleteSubscriber(asset.getId(), importUser.getId());
                                        }
                                        else{
                                            throw new Exception("There are multiple instances of the username '" + asset.getImportedByUsername() + "'!");
                                        }
                                    }
                                    else{
                                        throw new Exception("Username '" + asset.getImportedByUsername() + "' cannot be found in the system!");
                                    }
                                }

                                genericResponse.setMessage(selectedAssets.getSelectedAssets().size() + " asset(s) deleted successfully.");

                                _logger.debug("<< deleteAssets()");
                                return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                            }
                            else{
                                throw new Exception("There are no assets to set as deleted!");
                            }
                        }
                        else{
                            String warningMessage = "No assets were selected!";
                            _logger.warn(warningMessage);

                            genericResponse.setMessage(warningMessage);

                            _logger.debug("<< deleteAssets()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                        }
                    }
                    else{
                        throw new Exception("User is null!");
                    }
                }
                else{
                    String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< deleteAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< deleteAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while deleting assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< deleteAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/subscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> subscribeToAssets(Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("subscribeToAssets() >>");
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(selectedAssets != null){
                if(!selectedAssets.getSelectedAssets().isEmpty()){
                    if(isSessionValid(authentication)){
                        List<User> users = usersRepository.findUsersByUsername(authentication.getName());
                        if(!users.isEmpty()){
                            if(users.size() == 1){
                                User user = users.get(0);

                                int assetCount = 0;
                                for(Asset selectedAsset: selectedAssets.getSelectedAssets()){
                                    if(!isSubscribed(user, selectedAsset)){
                                        List<Asset> assetsByOriginalAssetId = assetsRepository.getAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                        assetsByOriginalAssetId.forEach(asset -> assetUserRepository.insertSubscriber(asset.getId(), user.getId()));
                                        assetCount++;
                                    }
                                }

                                if(assetCount > 0){
                                    if(assetCount == selectedAssets.getSelectedAssets().size()){
                                        genericResponse.setMessage(assetCount + " asset(s) were subscribed successfully.");
                                    }
                                    else{
                                        genericResponse.setMessage(selectedAssets.getSelectedAssets().size() + " out of " + assetCount + " asset(s) were subscribed successfully. The rest of the assets were already subscribed.");
                                    }

                                    _logger.debug("<< subscribeToAssets()");
                                    return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                }
                                else{
                                    genericResponse.setMessage("Selected assets were already subscribed.");

                                    _logger.debug("<< subscribeToAssets()");
                                    return new ResponseEntity<>(genericResponse, HttpStatus.NO_CONTENT);
                                }
                            }
                            else{
                                throw new Exception("Multiple users found with username '" + authentication.getName() + "'.");
                            }
                        }
                        else{
                            throw new Exception("No user was found with username '" + authentication.getName() + "'.");
                        }
                    }
                    else{
                        String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< subscribeToAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String warningMessage = "No assets were selected!";
                    _logger.warn(warningMessage);

                    genericResponse.setMessage(warningMessage);

                    _logger.debug("<< subscribeToAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< subscribeToAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while subscribing to assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< subscribeToAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @RequestMapping(value = "/assets/unsubscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> unsubscribeFromAssets(Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("unsubscribeFromAssets() >>");
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(selectedAssets != null){
                if(!selectedAssets.getSelectedAssets().isEmpty()){
                    if(isSessionValid(authentication)){
                        List<User> users = usersRepository.findUsersByUsername(authentication.getName());
                        if(!users.isEmpty()){
                            if(users.size() == 1){
                                User user = users.get(0);

                                int assetCount = 0;
                                for(Asset selectedAsset: selectedAssets.getSelectedAssets()){
                                    if(isSubscribed(user, selectedAsset)){
                                        List<Asset> assetsByOriginalAssetId = assetsRepository.getAssetsByOriginalAssetId(selectedAsset.getOriginalAssetId());
                                        assetsByOriginalAssetId.forEach(asset -> assetUserRepository.deleteSubscriber(asset.getId(), user.getId()));
                                        assetCount++;
                                    }
                                }

                                if(assetCount > 0){
                                    if(assetCount == selectedAssets.getSelectedAssets().size()){
                                        genericResponse.setMessage(assetCount + " asset(s) were unsubscribed successfully.");
                                    }
                                    else{
                                        genericResponse.setMessage(selectedAssets.getSelectedAssets().size() + " out of " + assetCount + " asset(s) were unsubscribed successfully. The rest of the assets were already subscribed.");
                                    }

                                    _logger.debug("<< unsubscribeFromAssets()");
                                    return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                }
                                else{
                                    genericResponse.setMessage("Selected assets were already unsubscribed.");

                                    _logger.debug("<< unsubscribeFromAssets()");
                                    return new ResponseEntity<>(genericResponse, HttpStatus.NO_CONTENT);
                                }
                            }
                            else{
                                throw new Exception("Multiple users found with username '" + authentication.getName() + "'.");
                            }
                        }
                        else{
                            throw new Exception("No user was found with username '" + authentication.getName() + "'.");
                        }
                    }
                    else{
                        String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< unsubscribeFromAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No assets were selected!";
                    _logger.warn(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< unsubscribeFromAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< unsubscribeFromAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while unsubscribing from assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< unsubscribeFromAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/{assetId}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateAsset(Authentication authentication, HttpSession session, @RequestBody UpdateAssetRequest updateAssetRequest, @PathVariable String assetId){
        _logger.debug("updateAssets() >>");
        GenericResponse genericResponse = new GenericResponse();

        try {
            if(StringUtils.isNotBlank(assetId)){
                if(updateAssetRequest != null){
                    if(isSessionValid(authentication)){
                        User user = getUser(authentication);
                        if(user != null){
                            List<Asset> assetsById = assetsRepository.getAssetsById(assetId);
                            if(!assetsById.isEmpty()){
                                if(assetsById.size() == 1){
                                    Asset asset = assetsById.get(0);
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
                                                throw new Exception("File path " + versionedAsset.getPath() + " is not a valid file!");
                                            }
                                        }

                                        // Send notification
                                        String notification = "Asset '" + asset.getName() + "' is updated by '" + user.getUsername() + "'";
                                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                        sendNotificationRequest.setAsset(asset);
                                        sendNotificationRequest.setFromUser(user);
                                        sendNotificationRequest.setMessage(notification);
                                        sendNotification(sendNotificationRequest, session);

                                        String message = "Asset updated successfully.";
                                        _logger.debug(message);

                                        genericResponse.setMessage(message);

                                        _logger.debug("<< updateAssets()");
                                        return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                    }
                                    else{
                                        throw new Exception("There are not assets with the original asset ID '" + asset.getOriginalAssetId() + "'!");
                                    }
                                }
                                else{
                                    throw new Exception("Multiple assets with ID '" + assetId + "' found!");
                                }
                            }
                            else{
                                String errorMessage = "Asset with ID '" + assetId + "' is not found!";
                                _logger.error(errorMessage);

                                genericResponse.setMessage(errorMessage);

                                _logger.debug("<< updateAssets()");
                                return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                            }
                        }
                        else{
                            throw new Exception("User is null!");
                        }
                    }
                    else{
                        String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< updateAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "Update asset request is null!";
                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< updateAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Asset ID is blank!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< updateAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while updating from assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< updateAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/{assetId}/versions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssetVersionResponse> getVersionHistory(Authentication authentication, @PathVariable String assetId){
        _logger.debug("getVersionHistory() >>");
        AssetVersionResponse assetVersionResponse = new AssetVersionResponse();

        try{
            if(StringUtils.isNotBlank(assetId)){
                if(isSessionValid(authentication)){
                    User user = getUser(authentication);
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

                                    _logger.debug("<< getVersionHistory()");
                                    return new ResponseEntity<>(assetVersionResponse, HttpStatus.OK);
                                }
                                else{
                                    throw new Exception("No assets found with original asset ID '" + asset.getOriginalAssetId() + "'!");
                                }
                            }
                            else{
                                throw new Exception("There are multiple assets with ID '" + assetId + "'!");
                            }
                        }
                        else{
                            String errorMessage = "No assets found with ID '" + assetId + "'.";
                            _logger.error(errorMessage);

                            assetVersionResponse.setMessage(errorMessage);

                            _logger.debug("<< getVersionHistory()");
                            return new ResponseEntity<>(assetVersionResponse, HttpStatus.NOT_FOUND);
                        }
                    }
                    else{
                        throw new Exception("User is null!");
                    }
                }
                else{
                    String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                    _logger.error(errorMessage);

                    assetVersionResponse.setMessage(errorMessage);

                    _logger.debug("<< updateAssets()");
                    return new ResponseEntity<>(assetVersionResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Asset ID is blank!";
                _logger.error(errorMessage);

                assetVersionResponse.setMessage(errorMessage);

                _logger.debug("<< getVersionHistory()");
                return new ResponseEntity<>(assetVersionResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving asset version history for asset with ID '" + assetId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            assetVersionResponse.setMessage(errorMessage);

            _logger.debug("<< getVersionHistory()");
            return new ResponseEntity<>(assetVersionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/{assetId}/revert", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> revertAssetToVersion(Authentication authentication, HttpSession session, @PathVariable String assetId, @RequestBody RevertAssetVersionRequest revertAssetVersionRequest){
        _logger.debug("revertAssetToVersion() >>");
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(StringUtils.isNotBlank(assetId)){
                if(revertAssetVersionRequest != null){
                    if(isSessionValid(authentication)){
                        List<Asset> assetsById = assetsRepository.getAssetsById(assetId);
                        if(!assetsById.isEmpty()){
                            if(assetsById.size() == 1){
                                Asset asset = assetsById.get(0);
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
                                        assetsRepository.updateAssetsLatestVersion("N", assetsToDelete.stream().map(a -> a.getId()).collect(Collectors.toList()));

                                        SelectedAssets selectedAssets = new SelectedAssets();
                                        selectedAssets.setSelectedAssets(assetsToDelete);

                                        RestTemplate restTemplate = new RestTemplate();

                                        HttpHeaders headers = getHeaders(session);
                                        HttpEntity<SelectedAssets> selectedAssetsEntity = new HttpEntity<>(selectedAssets, headers);

                                        List<ServiceInstance> instances = discoveryClient.getInstances(assetServiceLoadBalancerServiceName);

                                        if(!instances.isEmpty()){
                                            ServiceInstance serviceInstance = instances.get(0);
                                            String assetServiceUrl = serviceInstance.getUri().toString();
                                            ResponseEntity<GenericResponse> genericResponseResponseEntity = restTemplate.postForEntity(assetServiceUrl + "/assets/delete", selectedAssetsEntity, GenericResponse.class);
                                            boolean successful = genericResponseResponseEntity.getStatusCode().is2xxSuccessful();

                                            if(successful){
                                                genericResponse.setMessage("Asset was successfully reverted to version " + revertAssetVersionRequest.getVersion() + ".");

                                                _logger.debug("<< revertAssetToVersion()");
                                                return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                                            }
                                            else{
                                                throw new Exception("Higher version assets failed to be set as deleted. " + genericResponseResponseEntity.getBody().getMessage());
                                            }
                                        }
                                        else{
                                            throw new Exception("There is no asset load balancer instance!");
                                        }
                                    }
                                    else{
                                        String errorMessage = "There were no related assets with version higher than '" + revertAssetVersionRequest.getVersion() + "'.";
                                        _logger.error(errorMessage);

                                        genericResponse.setMessage(errorMessage);

                                        _logger.debug("<< revertAssetToVersion()");
                                        return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                                    }
                                }
                                else{
                                    throw new Exception("No assets with original asset ID '" + asset.getOriginalAssetId() + "' found!");
                                }
                            }
                            else{
                                throw new Exception("Multiple assets with ID '" + assetId + "' found!");
                            }
                        }
                        else{
                            throw new Exception("No assets with ID '" + assetId + "' found!");
                        }
                    }
                    else{
                        String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< revertAssetToVersion()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "Revert asset version request is null!";
                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< revertAssetToVersion()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Asset ID is blank!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< revertAssetToVersion()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while reverting the asset with ID '" + assetId + "' to version " + revertAssetVersionRequest.getVersion() + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< revertAssetToVersion()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private File getArchiveFile(long jobId, HttpHeaders headers, String jobServiceUrl) {
        _logger.debug("getArchiveFile() >>");

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<RetrieveToyboxJobResult> retrieveToyboxJobResultResponseEntity = restTemplate.exchange(jobServiceUrl + "/jobs/" + jobId, HttpMethod.GET, new HttpEntity<>(headers), RetrieveToyboxJobResult.class);
        RetrieveToyboxJobResult retrieveToyboxJobResult = retrieveToyboxJobResultResponseEntity.getBody();
        if(retrieveToyboxJobResult.getToyboxJob().getStatus().equalsIgnoreCase("COMPLETED")){
            String downloadFilePath =  exportStagingPath + File.separator + jobId + File.separator + "Download.zip";
            File file = new File(downloadFilePath);
            _logger.debug("<< getArchiveFile()");
            return file;
        }
        else if(retrieveToyboxJobResult.getToyboxJob().getStatus().equalsIgnoreCase("FAILED")){
            _logger.info("Job with ID '" + jobId + "' failed.");
            _logger.debug("<< getArchiveFile()");
            return null;
        }
        else{
            return getArchiveFile(jobId, headers, jobServiceUrl);
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
        String errorMessage;
        List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
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
        return false;
    }

    private User getUser(Authentication authentication){
        String errorMessage;
        List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
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
        return null;
    }

    private boolean isSubscribed(User user, Asset asset){
        List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());
        for(AssetUser assetUser: assetUsersByUserId){
            if(assetUser.getAssetId().equalsIgnoreCase(asset.getId())){
                return true;
            }
        }

        return false;
    }

    private boolean isJobSuccessful(long jobId, HttpHeaders headers, String jobServiceUrl) {
        _logger.debug("isJobSuccessful() >>");
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<RetrieveToyboxJobResult> retrieveToyboxJobResultResponseEntity = restTemplate.exchange(jobServiceUrl + "/jobs/" + jobId, HttpMethod.GET, new HttpEntity<>(headers), RetrieveToyboxJobResult.class);
        RetrieveToyboxJobResult retrieveToyboxJobResult = retrieveToyboxJobResultResponseEntity.getBody();
        if(retrieveToyboxJobResult.getToyboxJob().getStatus().equalsIgnoreCase("COMPLETED")){
            return true;
        }
        else if(retrieveToyboxJobResult.getToyboxJob().getStatus().equalsIgnoreCase("FAILED")){
            _logger.info("Job with ID '" + jobId + "' failed.");
            _logger.debug("<< isJobSuccessful()");
            return false;
        }
        else{
            return isJobSuccessful(jobId, headers, jobServiceUrl);
        }
    }
}
