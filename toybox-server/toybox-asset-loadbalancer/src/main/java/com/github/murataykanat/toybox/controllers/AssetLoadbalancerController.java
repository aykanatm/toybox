package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.asset.RetrieveAssetsResults;
import com.github.murataykanat.toybox.schema.asset.SelectedAssets;
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
    private RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "downloadAssetsErrorFallback")
    @RequestMapping(value = "/assets/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAssets(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("downloadAssets() >>");
        try {
            if(selectedAssets != null){
                if(session != null){
                    _logger.debug("Session ID: " + session.getId());
                    CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                    if(token != null){
                        _logger.debug("CSRF Token: " + token.getToken());

                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                        headers.set("X-XSRF-TOKEN", token.getToken());

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
                        String errorMessage = "CSRF token is null!";
                        _logger.error(errorMessage);

                        _logger.debug("<< downloadAssets()");
                        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "Session is null!";
                    _logger.error(errorMessage);

                    _logger.debug("<< downloadAssets()");
                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                _logger.debug("<< downloadAssets()");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading the assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< getLoadBalancedRendition()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Resource> downloadAssetsErrorFallback(HttpSession session, SelectedAssets selectedAssets, Throwable e){
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
    public ResponseEntity<GenericResponse> uploadAssets(Authentication authentication, HttpSession session, @RequestParam("upload") MultipartFile[] files) {
        _logger.debug("uploadAssets() >>");
        String tempFolderName = Long.toString(System.currentTimeMillis());
        String tempImportStagingPath = importStagingPath + File.separator + tempFolderName;
        _logger.debug("Import staging path: " + tempImportStagingPath);

        try{
            if(files != null){
                if(session != null){
                    if(authentication != null){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

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

                            uploadFileLst.setUploadFiles(uploadFiles);
                            uploadFileLst.setMessage("Files uploaded successfully!");

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< uploadAssets()");
                                return restTemplate.exchange(prefix + assetServiceName + "/assets/upload", HttpMethod.POST, new HttpEntity<>(uploadFileLst, headers), GenericResponse.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                GenericResponse genericResponse = new GenericResponse();
                                genericResponse.setMessage(errorMessage);

                                _logger.debug("<< stopJob()");
                                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String errorMessage = "Token is null!";

                            _logger.error(errorMessage);

                            GenericResponse genericResponse = new GenericResponse();
                            genericResponse.setMessage(errorMessage);

                            _logger.debug("<< uploadAssets()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Authentication is null!";

                        _logger.error(errorMessage);

                        GenericResponse genericResponse = new GenericResponse();
                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< uploadAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "Session is null!";

                    _logger.error(errorMessage);

                    GenericResponse genericResponse = new GenericResponse();
                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< uploadAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else{
                String errorMessage = "Files are null!";

                _logger.error(errorMessage);

                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setMessage(errorMessage);

                _logger.debug("<< uploadAssets()");
                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
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

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< uploadAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> uploadAssetsErrorFallback(Authentication authentication, HttpSession session, MultipartFile[] files, Throwable e){
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

            _logger.debug("<< downloadAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "retrieveAssetsErrorFallback")
    @RequestMapping(value = "/assets/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveAssetsResults> retrieveAssets(Authentication authentication, HttpSession session, @RequestBody AssetSearchRequest assetSearchRequest){
        _logger.debug("retrieveAssets() >>");
        try{
            if(assetSearchRequest != null){
                if(session != null){
                    if(authentication != null){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< retrieveAssets()");
                                return restTemplate.exchange(prefix + assetServiceName + "/assets/search", HttpMethod.POST, new HttpEntity<>(assetSearchRequest, headers), RetrieveAssetsResults.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";

                                _logger.error(errorMessage);

                                RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
                                retrieveAssetsResults.setMessage(errorMessage);

                                _logger.debug("<< uploadAssets()");
                                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String errorMessage = "Token is null!";

                            _logger.error(errorMessage);

                            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
                            retrieveAssetsResults.setMessage(errorMessage);

                            _logger.debug("<< retrieveAssets()");
                            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Authentication is null!";

                        _logger.error(errorMessage);

                        RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
                        retrieveAssetsResults.setMessage(errorMessage);

                        _logger.debug("<< retrieveAssets()");
                        return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "Session is null!";

                    _logger.error(errorMessage);

                    RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
                    retrieveAssetsResults.setMessage(errorMessage);

                    _logger.debug("<< retrieveAssets()");
                    return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Asset search request is null!";

                _logger.error(errorMessage);

                RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
                retrieveAssetsResults.setMessage(errorMessage);

                _logger.debug("<< retrieveAssets()");
                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.BAD_REQUEST);
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
            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<GenericResponse> deleteAssets(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssets() >>");
        try {
            if(selectedAssets != null){
                if(session != null){
                    _logger.debug("Session ID: " + session.getId());
                    CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                    if(token != null){
                        _logger.debug("CSRF Token: " + token.getToken());

                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                        headers.set("X-XSRF-TOKEN", token.getToken());

                        String prefix = getPrefix();
                        if(StringUtils.isNotBlank(prefix)){
                            _logger.debug("<< deleteAssets()");
                            return restTemplate.exchange(prefix + assetServiceName + "/assets/delete", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), GenericResponse.class);
                        }
                        else{
                            String errorMessage = "Service ID prefix is null!";
                            _logger.error(errorMessage);

                            GenericResponse genericResponse = new GenericResponse();
                            genericResponse.setMessage(errorMessage);

                            _logger.debug("<< stopJob()");
                            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                    else{
                        String errorMessage = "Token is null!";

                        _logger.error(errorMessage);

                        GenericResponse genericResponse = new GenericResponse();
                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< deleteAssets()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "Session is null!";

                    _logger.error(errorMessage);

                    GenericResponse genericResponse = new GenericResponse();
                    genericResponse.setMessage(errorMessage);

                    _logger.debug("<< deleteAssets()");
                    return new ResponseEntity<>(genericResponse, HttpStatus.UNAUTHORIZED);
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
            String errorMessage = "An error occurred while searching for assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< retrieveAssets()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<GenericResponse> deleteAssetsErrorFallback(HttpSession session, SelectedAssets selectedAssets, Throwable e){
        _logger.debug("downloadAssetsErrorFallback() >>");

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

            _logger.debug("<< downloadAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            _logger.debug("<< downloadAssetsErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
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
