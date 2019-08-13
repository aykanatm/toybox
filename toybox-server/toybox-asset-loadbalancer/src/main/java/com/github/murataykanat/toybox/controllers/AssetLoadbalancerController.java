package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.*;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RibbonClient(name = "toybox-asset-loadbalancer")
@RestController
public class AssetLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(AssetLoadbalancerController.class);

    private static final String assetServiceName = "toybox-asset-service";

    @Value("${importStagingPath}")
    private String importStagingPath;

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
    }

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RestTemplate restTemplate;

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "downloadAssetsErrorFallback")
    @RequestMapping(value = "/assets/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< downloadAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/download", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), Resource.class);
                    }
                    else{
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selected assets are null!";
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
            String errorMessage = "An error occurred while downloading the assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<Resource> downloadAssetsErrorFallback(Authentication authentication, HttpSession session, SelectedAssets selectedAssets, Throwable e){
        if(selectedAssets != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable download selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";
            _logger.error(errorMessage);

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // The name "upload" must match the "name" attribute of the input in UI
    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "uploadAssetsErrorFallback")
    @RequestMapping(value = "/assets/upload", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> uploadAssets(Authentication authentication, HttpSession session, @RequestHeader(value="container-id") String containerId, @RequestParam("upload") MultipartFile[] files) {
        String tempFolderName = Long.toString(System.currentTimeMillis());
        String tempImportStagingPath = importStagingPath + File.separator + tempFolderName;
        _logger.debug("Import staging path: " + tempImportStagingPath);

        GenericResponse genericResponse = new GenericResponse();
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(files != null){
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);
                    HttpHeaders headers =AuthenticationUtils.getInstance().getHeaders(session);

                    if(StringUtils.isNotBlank(prefix)){
                        File tempFolder = new File(tempImportStagingPath);
                        if(!tempFolder.exists()){
                            tempFolder.mkdir();
                        }

                        UploadFileLst uploadFileLst = new UploadFileLst();
                        List<UploadFile> uploadFiles = new ArrayList<>();

                        for(MultipartFile file: files){
                            String path;
                            if(tempFolder.exists()){
                                path = tempFolder.getAbsolutePath() + File.separator + file.getOriginalFilename();
                            }
                            else{
                                throw new FileNotFoundException("The temp folder " + tempFolder.getAbsolutePath() + " does not exist!");
                            }

                            file.transferTo(new File(path));

                            UploadFile uploadFile = new UploadFile();
                            uploadFile.setPath(path);
                            uploadFile.setUsername(authentication.getName());

                            uploadFiles.add(uploadFile);
                        }

                        uploadFileLst.setContainerId(containerId);
                        uploadFileLst.setUploadFiles(uploadFiles);
                        uploadFileLst.setMessage("Files uploaded successfully!");

                        return restTemplate.exchange(prefix + assetServiceName + "/assets/upload", HttpMethod.POST, new HttpEntity<>(uploadFileLst, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Files are null!";

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
            String errorMessage = "An error occurred while uploading files. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            File tempFolder = new File(tempImportStagingPath);
            try {
                if(tempFolder.exists()){
                    FileUtils.deleteDirectory(tempFolder);
                }
            }
            catch (IOException ioe){
                String ioErrorMessage = ". An error occurred while deleting the temp files. " + ioe.getLocalizedMessage();
                errorMessage += ioErrorMessage;
            }

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> uploadAssetsErrorFallback(Authentication authentication, HttpSession session, String containerId, MultipartFile[] files, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(files != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to upload assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Files are null!";

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "retrieveAssetsErrorFallback")
    @RequestMapping(value = "/assets/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveAssetsResults> retrieveAssets(Authentication authentication, HttpSession session, @RequestBody AssetSearchRequest assetSearchRequest){
        try{
            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();

            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(assetSearchRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< retrieveAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/search", HttpMethod.POST, new HttpEntity<>(assetSearchRequest, headers), RetrieveAssetsResults.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";

                        _logger.error(errorMessage);

                        retrieveAssetsResults.setMessage(errorMessage);

                        return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Asset search request is null!";

                    _logger.error(errorMessage);

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
            String errorMessage = "An error occurred while searching for assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveAssetsResults> retrieveAssetsErrorFallback(Authentication authentication, HttpSession session, AssetSearchRequest assetSearchRequest, Throwable e){
        if(assetSearchRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to retrieve assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Asset search request is null!";

            _logger.error(errorMessage);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "deleteAssetsErrorFallback")
    @RequestMapping(value = "/assets/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> deleteAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        try {
            GenericResponse genericResponse = new GenericResponse();
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/delete", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selected assets are null!";

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

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> deleteAssetsErrorFallback(Authentication authentication, HttpSession session, SelectedAssets selectedAssets, Throwable e){
        if(selectedAssets != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to delete selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "subscribeToAssetsErrorFallback")
    @RequestMapping(value = "/assets/subscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> subscribeToAssets(HttpSession session, Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        try{
            GenericResponse genericResponse = new GenericResponse();

            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/subscribe", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selected assets are null!";

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

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> subscribeToAssetsErrorFallback(HttpSession session, Authentication authentication, SelectedAssets selectedAssets, Throwable e){
        if(selectedAssets != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to subscribe to the selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "unsubscribeFromAssetsErrorFallback")
    @RequestMapping(value = "/assets/unsubscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> unsubscribeFromAssets(HttpSession session, Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        try {
            GenericResponse genericResponse = new GenericResponse();

            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/unsubscribe", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selected assets are null!";
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

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> unsubscribeFromAssetsErrorFallback(HttpSession session, Authentication authentication, SelectedAssets selectedAssets, Throwable e){
        if(selectedAssets != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to unsubscribe from the selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "updateAssetErrorFallback")
    @RequestMapping(value = "/assets/{assetId}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateAsset(Authentication authentication, HttpSession session, @RequestBody UpdateAssetRequest updateAssetRequest, @PathVariable String assetId){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(updateAssetRequest != null){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);

                        if(StringUtils.isNotBlank(prefix)){
                            return restTemplate.exchange(prefix + assetServiceName + "/assets/" + assetId, HttpMethod.PATCH, new HttpEntity<>(updateAssetRequest, headers), GenericResponse.class);
                        }
                        else{
                            String errorMessage = "Service ID prefix is null!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
                    String errorMessage = "Asset ID is blank";

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
            String errorMessage = "An error occurred while updating assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> updateAssetErrorFallback(Authentication authentication, HttpSession session, UpdateAssetRequest updateAssetRequest, String assetId, Throwable e){
        if(updateAssetRequest != null){
            if(StringUtils.isNotBlank(assetId)){
                String errorMessage;
                if(e.getLocalizedMessage() != null){
                    errorMessage = "Unable to update asset. " + e.getLocalizedMessage();
                }
                else{
                    errorMessage = "Unable to get response from the asset service.";
                }

                _logger.error(errorMessage, e);

                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Asset ID is blank!";

                _logger.error(errorMessage);

                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Update asset request is null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "getVersionHistoryErrorFallback")
    @RequestMapping(value = "/assets/{assetId}/versions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssetVersionResponse> getVersionHistory(Authentication authentication, HttpSession session, @PathVariable String assetId){
        AssetVersionResponse assetVersionResponse = new AssetVersionResponse();

        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/" + assetId + "/versions", HttpMethod.GET, new HttpEntity<>(headers), AssetVersionResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        assetVersionResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(assetVersionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
    public ResponseEntity<AssetVersionResponse> getVersionHistoryErrorFallback(Authentication authentication, HttpSession session, String assetId, Throwable e){
        AssetVersionResponse assetVersionResponse = new AssetVersionResponse();
        if(StringUtils.isNotBlank(assetId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to retrieve the asset version history. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            assetVersionResponse.setMessage(errorMessage);

            return new ResponseEntity<>(assetVersionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Asset ID is blank!";

            _logger.error(errorMessage);

            assetVersionResponse.setMessage(errorMessage);

            return new ResponseEntity<>(assetVersionResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "revertAssetToVersionErrorFallback")
    @RequestMapping(value = "/assets/{assetId}/revert", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> revertAssetToVersion(Authentication authentication, HttpSession session, @PathVariable String assetId, @RequestBody RevertAssetVersionRequest revertAssetVersionRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(revertAssetVersionRequest != null){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);
                        if(StringUtils.isNotBlank(prefix)){
                            return restTemplate.exchange(prefix + assetServiceName + "/assets/" + assetId + "/revert", HttpMethod.POST, new HttpEntity<>(revertAssetVersionRequest, headers), GenericResponse.class);
                        }
                        else{
                            String errorMessage = "Service ID prefix is null!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
                errorMessage = "An error occurred while reverting the asset with ID '" + assetId + ". " + e.getLocalizedMessage();
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> revertAssetToVersionErrorFallback(Authentication authentication, HttpSession session, String assetId, RevertAssetVersionRequest revertAssetVersionRequest, Throwable e){
        GenericResponse genericResponse = new GenericResponse();
        if(StringUtils.isNotBlank(assetId)){
            if(revertAssetVersionRequest != null){
                String errorMessage;
                if(e.getLocalizedMessage() != null){
                    errorMessage = "Unable to revert the asset to a previous version. " + e.getLocalizedMessage();
                }
                else{
                    errorMessage = "Unable to get response from the asset service.";
                }

                _logger.error(errorMessage, e);

                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "moveAssetErrorFallback")
    @RequestMapping(value = "/assets/move", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> moveAsset(Authentication authentication, HttpSession session, @RequestBody MoveAssetRequest moveAssetRequest){
        GenericResponse genericResponse = new GenericResponse();
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(moveAssetRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);
                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/move", HttpMethod.POST, new HttpEntity<>(moveAssetRequest, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
    public ResponseEntity<GenericResponse> moveAssetErrorFallback(Authentication authentication, HttpSession session, MoveAssetRequest moveAssetRequest, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(moveAssetRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to move the selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Move asset request is null!";
            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "copyAssetErrorFallback")
    @RequestMapping(value = "/assets/copy", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> copyAsset(Authentication authentication, HttpSession session, @RequestBody CopyAssetRequest copyAssetRequest){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(copyAssetRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, assetServiceName);
                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/copy", HttpMethod.POST, new HttpEntity<>(copyAssetRequest, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            String errorMessage = "An error occurred while moving assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> copyAssetErrorFallback(Authentication authentication, HttpSession session, CopyAssetRequest copyAssetRequest, Throwable e){
        GenericResponse genericResponse = new GenericResponse();

        if(copyAssetRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to move the selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Copy asset request is null!";
            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
