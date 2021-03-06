package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.ribbon.RibbonRetryHttpRequestFactory;
import com.github.murataykanat.toybox.schema.asset.*;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
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

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RestTemplate defaultRestTemplate;

    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;

    @Value("${importStagingPath}")
    private String importStagingPath;

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(SpringClientFactory springClientFactory, LoadBalancerClient loadBalancerClient){
        this.restTemplate = new RestTemplate();
        RibbonRetryHttpRequestFactory lFactory = new RibbonRetryHttpRequestFactory(springClientFactory, loadBalancerClient);
        restTemplate.setRequestFactory(lFactory);
        return restTemplate;
    }

    @LoadBalanced
    @Bean
    public RestTemplate defaultRestTemplate(RestTemplateBuilder restTemplateBuilder){
        return restTemplateBuilder.build();
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(files != null){
                    String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.ASSET_SERVICE_NAME);
                    HttpHeaders headers = authenticationUtils.getHeaders(session);

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

                        return defaultRestTemplate.exchange(prefix + ToyboxConstants.ASSET_SERVICE_NAME + "/assets/upload", HttpMethod.POST, new HttpEntity<>(uploadFileLst, headers), GenericResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
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
        RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(assetSearchRequest != null){
                    HttpHeaders headers = authenticationUtils.getHeaders(session);
                    String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.ASSET_SERVICE_NAME);

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< retrieveAssets()");
                        return restTemplate.exchange(prefix + ToyboxConstants.ASSET_SERVICE_NAME + "/assets/search", HttpMethod.POST, new HttpEntity<>(assetSearchRequest, headers), RetrieveAssetsResults.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            retrieveAssetsResults.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(retrieveAssetsResults, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while searching for assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

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
    @HystrixCommand(fallbackMethod = "updateAssetErrorFallback")
    @RequestMapping(value = "/assets/{assetId}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateAsset(Authentication authentication, HttpSession session, @RequestBody UpdateAssetRequest updateAssetRequest, @PathVariable String assetId){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(updateAssetRequest != null){
                        HttpHeaders headers = authenticationUtils.getHeaders(session);
                        String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.ASSET_SERVICE_NAME);

                        if(StringUtils.isNotBlank(prefix)){
                            // TODO:
                            //  RibbonRetryHttpRequestFactory does not support PATCH requests (https://github.com/Netflix/ribbon/issues/285)
                            //  Issue will be fixed when the project is upgraded to the latest version of Spring Cloud
                            return defaultRestTemplate.exchange(prefix + ToyboxConstants.ASSET_SERVICE_NAME + "/assets/" + assetId, HttpMethod.PATCH, new HttpEntity<>(updateAssetRequest, headers), GenericResponse.class);
                        }
                        else{
                            throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    HttpHeaders headers = authenticationUtils.getHeaders(session);
                    String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.ASSET_SERVICE_NAME);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + ToyboxConstants.ASSET_SERVICE_NAME + "/assets/" + assetId + "/versions", HttpMethod.GET, new HttpEntity<>(headers), AssetVersionResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            assetVersionResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(assetVersionResponse, httpEx.getStatusCode());
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
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(assetId)){
                    if(revertAssetVersionRequest != null){
                        HttpHeaders headers = authenticationUtils.getHeaders(session);
                        String prefix = loadbalancerUtils.getPrefix(ToyboxConstants.ASSET_SERVICE_NAME);
                        if(StringUtils.isNotBlank(prefix)){
                            return restTemplate.exchange(prefix + ToyboxConstants.ASSET_SERVICE_NAME + "/assets/" + assetId + "/revert", HttpMethod.POST, new HttpEntity<>(revertAssetVersionRequest, headers), GenericResponse.class);
                        }
                        else{
                            throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            genericResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(genericResponse, httpEx.getStatusCode());
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
}