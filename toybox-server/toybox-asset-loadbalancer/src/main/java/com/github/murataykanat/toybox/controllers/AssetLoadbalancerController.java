package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.*;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
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

    @HystrixCommand(fallbackMethod = "downloadAssetsErrorFallback")
    @RequestMapping(value = "/assets/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("downloadAssets() >>");
        try {
            if(isSessionValid(authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< downloadAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/download", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), Resource.class);
                    }
                    else{
                        _logger.debug("<< downloadAssets()");
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selected assets are null!";
                    _logger.error(errorMessage);

                    _logger.debug("<< downloadAssets()");
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                _logger.debug("<< downloadAssets()");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading the assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< downloadAssets()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Resource> downloadAssetsErrorFallback(Authentication authentication, HttpSession session, SelectedAssets selectedAssets, Throwable e){
        _logger.debug("downloadAssetsErrorFallback() >>");

        if(selectedAssets != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable download selected assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            _logger.debug("<< downloadAssetsErrorFallback()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";
            _logger.error(errorMessage);

            _logger.debug("<< downloadAssetsErrorFallback()");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // The name "upload" must match the "name" attribute of the input in UI
    @HystrixCommand(fallbackMethod = "uploadAssetsErrorFallback")
    @RequestMapping(value = "/assets/upload", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> uploadAssets(Authentication authentication, HttpSession session, @RequestHeader(value="container-id") String containerId, @RequestParam("upload") MultipartFile[] files) {
        _logger.debug("uploadAssets() >>");
        String tempFolderName = Long.toString(System.currentTimeMillis());
        String tempImportStagingPath = importStagingPath + File.separator + tempFolderName;
        _logger.debug("Import staging path: " + tempImportStagingPath);

        GenericResponse genericResponse = new GenericResponse();
        try{
            if(isSessionValid(authentication)){
                if(files != null){
                    String prefix = getPrefix();
                    HttpHeaders headers = getHeaders(session);

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

                        _logger.debug("<< uploadAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/upload", HttpMethod.POST, new HttpEntity<>(uploadFileLst, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< uploadAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Files are null!";

                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< uploadAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< uploadAssets()");
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

            _logger.debug("<< uploadAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> uploadAssetsErrorFallback(Authentication authentication, HttpSession session, String containerId, MultipartFile[] files, Throwable e){
        _logger.debug("uploadAssetsErrorFallback() >>");

        if(files != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to upload assets. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the asset service.";
            }

            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< uploadAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Files are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< uploadAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "retrieveAssetsErrorFallback")
    @RequestMapping(value = "/assets/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveAssetsResults> retrieveAssets(Authentication authentication, HttpSession session, @RequestBody AssetSearchRequest assetSearchRequest){
        _logger.debug("retrieveAssets() >>");
        try{
            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();

            if(isSessionValid(authentication)){
                if(assetSearchRequest != null){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< retrieveAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/search", HttpMethod.POST, new HttpEntity<>(assetSearchRequest, headers), RetrieveAssetsResults.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";

                        _logger.error(errorMessage);

                        retrieveAssetsResults.setMessage(errorMessage);

                        _logger.debug("<< retrieveAssets()");
                        return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Asset search request is null!";

                    _logger.error(errorMessage);

                    retrieveAssetsResults.setMessage(errorMessage);

                    _logger.debug("<< retrieveAssets()");
                    return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.BAD_REQUEST);
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
        catch (Exception e){
            String errorMessage = "An error occurred while searching for assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            _logger.debug("<< retrieveAssets()");
            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<RetrieveAssetsResults> retrieveAssetsErrorFallback(Authentication authentication, HttpSession session, AssetSearchRequest assetSearchRequest, Throwable e){
        _logger.debug("retrieveAssetsErrorFallback() >>");

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

            _logger.debug("<< retrieveAssetsErrorFallback()");
            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Asset search request is null!";

            _logger.error(errorMessage);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            _logger.debug("<< retrieveAssetsErrorFallback()");
            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "deleteAssetsErrorFallback")
    @RequestMapping(value = "/assets/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> deleteAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssets() >>");
        try {
            GenericResponse genericResponse = new GenericResponse();

            if(isSessionValid(authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< deleteAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/delete", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< deleteAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< deleteAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
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

    public ResponseEntity<GenericResponse> deleteAssetsErrorFallback(Authentication authentication, HttpSession session, SelectedAssets selectedAssets, Throwable e){
        _logger.debug("deleteAssetsErrorFallback() >>");

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

            _logger.debug("<< deleteAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< deleteAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "subscribeToAssetsErrorFallback")
    @RequestMapping(value = "/assets/subscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> subscribeToAssets(HttpSession session, Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("subscribeToAssets() >>");
        try{
            GenericResponse genericResponse = new GenericResponse();

            if(isSessionValid(authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< subscribeToAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/subscribe", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< subscribeToAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< subscribeToAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
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

    public ResponseEntity<GenericResponse> subscribeToAssetsErrorFallback(HttpSession session, Authentication authentication, SelectedAssets selectedAssets, Throwable e){
        _logger.debug("subscribeToAssetsErrorFallback() >>");

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

            _logger.debug("<< subscribeToAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< subscribeToAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "unsubscribeFromAssetsErrorFallback")
    @RequestMapping(value = "/assets/unsubscribe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> unsubscribeFromAssets(HttpSession session, Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("unsubscribeFromAssets() >>");
        try {
            GenericResponse genericResponse = new GenericResponse();

            if(isSessionValid(authentication)){
                if(selectedAssets != null){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< unsubscribeFromAssets()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/unsubscribe", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< unsubscribeFromAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< unsubscribeFromAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while unsubscribing from assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< unsubscribeFromAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> unsubscribeFromAssetsErrorFallback(HttpSession session, Authentication authentication, SelectedAssets selectedAssets, Throwable e){
        _logger.debug("unsubscribeFromAssetsErrorFallback() >>");

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

            _logger.debug("<< unsubscribeFromAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< unsubscribeFromAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "updateAssetErrorFallback")
    @RequestMapping(value = "/assets/{assetId}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateAsset(Authentication authentication, HttpSession session, @RequestBody UpdateAssetRequest updateAssetRequest, @PathVariable String assetId){
        _logger.error("updateAsset()");
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(updateAssetRequest != null){
                        HttpHeaders headers = getHeaders(session);
                        String prefix = getPrefix();

                        if(StringUtils.isNotBlank(prefix)){
                            _logger.debug("<< updateAsset()");
                            return restTemplate.exchange(prefix + assetServiceName + "/assets/" + assetId, HttpMethod.PATCH, new HttpEntity<>(updateAssetRequest, headers), GenericResponse.class);
                        }
                        else{
                            String errorMessage = "Service ID prefix is null!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            _logger.debug("<< updateAsset()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                    else{
                        String errorMessage = "Update asset request is null!";

                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< updateAsset()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Asset ID is blank";

                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< updateAsset()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< updateAsset()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while updating assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< updateAsset()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> updateAssetErrorFallback(Authentication authentication, HttpSession session, UpdateAssetRequest updateAssetRequest, String assetId, Throwable e){
        _logger.debug("updateAssetErrorFallback() >>");

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

                _logger.debug("<< updateAssetErrorFallback()");
                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Asset ID is blank!";

                _logger.error(errorMessage);

                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setMessage(errorMessage);

                _logger.debug("<< updateAssetErrorFallback()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Update asset request is null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< updateAssetErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "getVersionHistoryErrorFallback")
    @RequestMapping(value = "/assets/{assetId}/versions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssetVersionResponse> getVersionHistory(Authentication authentication, HttpSession session, @PathVariable String assetId){
        _logger.debug("getVersionHistory() >>");
        AssetVersionResponse assetVersionResponse = new AssetVersionResponse();

        try {
            if(isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< updateAsset()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/" + assetId + "/versions", HttpMethod.GET, new HttpEntity<>(headers), AssetVersionResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        assetVersionResponse.setMessage(errorMessage);

                        _logger.debug("<< getVersionHistory()");
                        return new ResponseEntity<>(assetVersionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                assetVersionResponse.setMessage(errorMessage);

                _logger.debug("<< updateAssets()");
                return new ResponseEntity<>(assetVersionResponse, HttpStatus.UNAUTHORIZED);
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

    public ResponseEntity<AssetVersionResponse> getVersionHistoryErrorFallback(Authentication authentication, HttpSession session, String assetId, Throwable e){
        _logger.debug("getVersionHistoryErrorFallback() >>");

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

            _logger.debug("<< getVersionHistoryErrorFallback()");
            return new ResponseEntity<>(assetVersionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Asset ID is blank!";

            _logger.error(errorMessage);

            assetVersionResponse.setMessage(errorMessage);

            _logger.debug("<< getVersionHistoryErrorFallback()");
            return new ResponseEntity<>(assetVersionResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "revertAssetToVersionErrorFallback")
    @RequestMapping(value = "/assets/{assetId}/revert", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> revertAssetToVersion(Authentication authentication, HttpSession session, @PathVariable String assetId, @RequestBody RevertAssetVersionRequest revertAssetVersionRequest){
        _logger.debug("revertAssetToVersion() >>");
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(revertAssetVersionRequest != null){
                        HttpHeaders headers = getHeaders(session);
                        String prefix = getPrefix();
                        if(StringUtils.isNotBlank(prefix)){
                            _logger.debug("<< revertAssetToVersion()");
                            return restTemplate.exchange(prefix + assetServiceName + "/assets/" + assetId + "/revert", HttpMethod.POST, new HttpEntity<>(revertAssetVersionRequest, headers), GenericResponse.class);
                        }
                        else{
                            String errorMessage = "Service ID prefix is null!";
                            _logger.error(errorMessage);

                            genericResponse.setMessage(errorMessage);

                            _logger.debug("<< revertAssetToVersion()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< revertAssetToVersion()");
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

            _logger.debug("<< revertAssetToVersion()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> revertAssetToVersionErrorFallback(Authentication authentication, HttpSession session, String assetId, RevertAssetVersionRequest revertAssetVersionRequest, Throwable e){
        _logger.debug("revertAssetToVersionErrorFallback() >>");

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

                _logger.debug("<< revertAssetToVersionErrorFallback()");
                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Revert asset version request is null!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< revertAssetToVersionErrorFallback()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Asset ID is blank!";

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< revertAssetToVersionErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "moveAssetErrorFallback")
    @RequestMapping(value = "/assets/move", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> moveAsset(Authentication authentication, HttpSession session, @RequestBody MoveAssetRequest moveAssetRequest){
        _logger.debug("moveAsset() >>");

        GenericResponse genericResponse = new GenericResponse();
        try{
            if(isSessionValid(authentication)){
                if(moveAssetRequest != null){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();
                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< moveAsset()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/move", HttpMethod.POST, new HttpEntity<>(moveAssetRequest, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< moveAsset()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Asset move request is null!";
                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< moveAsset()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< moveAsset()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while moving assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< moveAsset()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> moveAssetErrorFallback(Authentication authentication, HttpSession session, MoveAssetRequest moveAssetRequest, Throwable e){
        _logger.debug("moveAssetErrorFallback() >>");

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

            _logger.debug("<< moveAssetErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Move asset request is null!";
            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< moveAssetErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "copyAssetErrorFallback")
    @RequestMapping(value = "/assets/copy", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> copyAsset(Authentication authentication, HttpSession session, @RequestBody CopyAssetRequest copyAssetRequest){
        _logger.debug("copyAsset() >>");
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(isSessionValid(authentication)){
                if(copyAssetRequest != null){
                    HttpHeaders headers = getHeaders(session);
                    String prefix = getPrefix();
                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< copyAsset()");
                        return restTemplate.exchange(prefix + assetServiceName + "/assets/copy", HttpMethod.POST, new HttpEntity<>(copyAssetRequest, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< copyAsset()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Copy move request is null!";
                    _logger.error(errorMessage);

                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< moveAsset()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                genericResponse.setMessage(errorMessage);

                _logger.debug("<< copyAsset()");
                return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while moving assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< copyAsset()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> copyAssetErrorFallback(Authentication authentication, HttpSession session, CopyAssetRequest copyAssetRequest, Throwable e){
        _logger.debug("copyAssetErrorFallback() >>");

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

            _logger.debug("<< copyAssetErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Copy asset request is null!";
            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< copyAssetErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
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

    private String getPrefix() throws Exception {
        _logger.debug("getPrefix() >>");
        List<ServiceInstance> instances = discoveryClient.getInstances(assetServiceName);
        if(!instances.isEmpty()){
            List<Boolean> serviceSecurity = new ArrayList<>();
            for(ServiceInstance serviceInstance: instances){
                serviceSecurity.add(serviceInstance.isSecure());
            }

            boolean result = serviceSecurity.get(0);

            for(boolean isServiceSecure : serviceSecurity){
                result ^= isServiceSecure;
            }

            if(!result){
                String prefix = result ? "https://" : "http://";

                _logger.debug("<< getPrefix() [" + prefix + "]");
                return prefix;
            }
            else{
                String errorMessage = "Not all asset services have the same transfer protocol!";
                _logger.error(errorMessage);

                throw new Exception(errorMessage);

            }
        }
        else{
            String errorMessage = "No asset services are running!";
            _logger.error(errorMessage);

            throw new Exception(errorMessage);
        }
    }
}
