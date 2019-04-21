package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.AssetUser;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.asset.RetrieveAssetsResults;
import com.github.murataykanat.toybox.schema.asset.SelectedAssets;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobResult;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import com.github.murataykanat.toybox.utilities.SortUtils;
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
    public ResponseEntity<Resource> downloadAssets(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("downloadAssets() >>");
        List<Asset> assets = selectedAssets.getSelectedAssets();
        try{
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
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        _logger.debug("CSRF Token: " + token.getToken());

                        RestTemplate restTemplate = new RestTemplate();
                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                        headers.set("X-XSRF-TOKEN", token.getToken());
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
                                    if(archiveFile.exists()){
                                        InputStreamResource resource = new InputStreamResource(new FileInputStream(archiveFile));

                                        _logger.debug("<< downloadAssets()");
                                        return new ResponseEntity<>(resource, HttpStatus.OK);
                                    }
                                    else{
                                        throw new IOException("File '" + archiveFile.getAbsolutePath() + "' does not exist!");
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
            _logger.debug("Session ID: " + session.getId());
            CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
            _logger.debug("CSRF Token: " + token.getToken());

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
            headers.set("X-XSRF-TOKEN", token.getToken());
            HttpEntity<UploadFileLst> selectedAssetsEntity = new HttpEntity<>(uploadFileLst, headers);
            List<ServiceInstance> instances = discoveryClient.getInstances(jobServiceLoadBalancerServiceName);
            if(!instances.isEmpty()){
                ServiceInstance serviceInstance = instances.get(0);
                String jobServiceUrl = serviceInstance.getUri().toString();
                ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/import", selectedAssetsEntity, JobResponse.class);
                boolean successful = jobResponseResponseEntity.getStatusCode().is2xxSuccessful();

                if(successful){
                    GenericResponse genericResponse = new GenericResponse();
                    genericResponse.setMessage(uploadFileLst.getUploadFiles().size() + " file(s) successfully uploaded. Import job started.");
                    _logger.debug("<< uploadAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.CREATED);
                }
                else{
                    GenericResponse genericResponse = new GenericResponse();
                    genericResponse.setMessage("Upload was successful but import failed to start. " + jobResponseResponseEntity.getBody().getMessage());
                    _logger.debug("<< uploadAssets()");
                    return new ResponseEntity<>(genericResponse, jobResponseResponseEntity.getStatusCode());
                }
            }
            else{
                throw new Exception("There is no job load balancer instance!");
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

                            List<Facet> facets = FacetUtils.getInstance().getFacets(assets);

                            retrieveAssetsResults.setFacets(facets);

                            if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
                                SortUtils.getInstance().sortItems(sortType, assets, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                                SortUtils.getInstance().sortItems(sortType, assets, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                            }

                            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

                            List<Asset> assetsByCurrentUser;
                            if(authorities.contains("ROLE_ADMIN")){
                                _logger.debug("Retrieving all assets [Admin User]...");
                                assetsByCurrentUser = assets;
                            }
                            else{
                                _logger.debug("Retrieving assets of the user '" + user.getUsername() + "'...");
                                assetsByCurrentUser = assets.stream().filter(a -> a.getImportedByUsername() != null && a.getImportedByUsername().equalsIgnoreCase(user.getUsername())).collect(Collectors.toList());
                            }

                            int totalRecords = assets.size();
                            int startIndex = offset;
                            int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                            List<Asset> assetsOnPage = assetsByCurrentUser.subList(startIndex, endIndex);

                            List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());

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
                            String message = "There is no asset to return.";
                            _logger.debug(message);

                            retrieveAssetsResults.setMessage(message);

                            _logger.debug("<< retrieveJobs()");
                            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.NO_CONTENT);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        retrieveAssetsResults.setMessage(errorMessage);

                        _logger.debug("<< retrieveJobs()");
                        return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    retrieveAssetsResults.setMessage(errorMessage);

                    _logger.debug("<< retrieveJobs()");
                    return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Asset search request is null!";
                _logger.debug(errorMessage);

                retrieveAssetsResults.setMessage(errorMessage);

                _logger.debug("<< retrieveJobs()");
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
    public ResponseEntity<GenericResponse> deleteAssets(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssets() >>");
        try{
            if(selectedAssets != null){
                if(!selectedAssets.getSelectedAssets().isEmpty()){
                    _logger.debug("Session ID: " + session.getId());
                    CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                    _logger.debug("CSRF Token: " + token.getToken());

                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                    headers.set("X-XSRF-TOKEN", token.getToken());
                    HttpEntity<SelectedAssets> selectedAssetsEntity = new HttpEntity<>(selectedAssets, headers);

                    List<ServiceInstance> instances = discoveryClient.getInstances(jobServiceLoadBalancerServiceName);
                    if(!instances.isEmpty()){
                        ServiceInstance serviceInstance = instances.get(0);
                        String jobServiceUrl = serviceInstance.getUri().toString();
                        ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/delete", selectedAssetsEntity, JobResponse.class);
                        long jobId = jobResponseResponseEntity.getBody().getJobId();

                        boolean jobSucceeded = isJobSuccessful(jobId, headers, jobServiceUrl);

                        if(jobSucceeded){
                            GenericResponse genericResponse = new GenericResponse();
                            genericResponse.setMessage(selectedAssets.getSelectedAssets().size() + " asset(s) deleted successfully.");

                            _logger.debug("<< deleteAssets()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            GenericResponse genericResponse = new GenericResponse();
                            genericResponse.setMessage("An error occurred while deleting assets.");

                            _logger.debug("<< deleteAssets()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                    else{
                        throw new Exception("There is no job load balancer instance!");
                    }
                }
                else{
                    String warningMessage = "No assets were selected!";
                    _logger.warn(warningMessage);

                    GenericResponse genericResponse = new GenericResponse();
                    genericResponse.setMessage(warningMessage);

                    _logger.debug("<< deleteAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                GenericResponse genericResponse = new GenericResponse();
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
    public ResponseEntity<GenericResponse> subscribeToAssets(HttpSession session, Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("subscribeToAssets() >>");
        try{
            if(selectedAssets != null){
                if(!selectedAssets.getSelectedAssets().isEmpty()){
                    List<User> users = usersRepository.findUsersByUsername(authentication.getName());
                    if(!users.isEmpty()){
                        if(users.size() == 1){
                            User user = users.get(0);
                            selectedAssets.getSelectedAssets().forEach(asset -> assetUserRepository.insertSubscriber(asset.getId(), user.getId()));

                            GenericResponse genericResponse = new GenericResponse();
                            genericResponse.setMessage(selectedAssets.getSelectedAssets().size() + " asset(s) were subscribed successfully.");

                            _logger.debug("<< subscribeToAssets()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.OK);
                        }
                        else{
                            String errorMessage = "Multiple users found with username '" + authentication.getName() + "'.";
                            _logger.error(errorMessage);

                            GenericResponse genericResponse = new GenericResponse();
                            genericResponse.setMessage(errorMessage);

                            _logger.debug("<< subscribeToAssets()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                    else{
                        String errorMessage = "No user was found with username '" + authentication.getName() + "'.";
                        _logger.error(errorMessage);

                        GenericResponse genericResponse = new GenericResponse();
                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< subscribeToAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String warningMessage = "No assets were selected!";
                    _logger.warn(warningMessage);

                    GenericResponse genericResponse = new GenericResponse();
                    genericResponse.setMessage(warningMessage);

                    _logger.debug("<< subscribeToAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setMessage(errorMessage);

                _logger.debug("<< subscribeToAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while subscribing to assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< subscribeToAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private File getArchiveFile(long jobId, HttpHeaders headers, String jobServiceUrl) throws Exception {
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
