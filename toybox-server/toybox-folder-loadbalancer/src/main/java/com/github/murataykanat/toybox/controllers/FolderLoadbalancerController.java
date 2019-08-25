package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.container.*;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-folder-loadbalancer")
@RestController
public class FolderLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(FolderLoadbalancerController.class);

    private static final String folderServiceName = "toybox-folder-service";

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
    @HystrixCommand(fallbackMethod = "createContainerErrorFallback")
    @RequestMapping(value = "/containers", method = RequestMethod.POST)
    public ResponseEntity<CreateContainerResponse> createContainer(Authentication authentication, HttpSession session, @RequestBody CreateContainerRequest createContainerRequest){
        CreateContainerResponse createContainerResponse = new CreateContainerResponse();
        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(createContainerRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, folderServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + folderServiceName + "/containers", HttpMethod.POST, new HttpEntity<>(createContainerRequest, headers), CreateContainerResponse.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
                    }
                }
                else{
                    String errorMessage = "Create container request is null!";

                    _logger.error(errorMessage);

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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            createContainerResponse.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(createContainerResponse, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while creating the container. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            createContainerResponse.setMessage(errorMessage);

            return new ResponseEntity<>(createContainerResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<CreateContainerResponse> createContainerErrorFallback(Authentication authentication, HttpSession session, CreateContainerRequest createContainerRequest, Throwable e){
        CreateContainerResponse createContainerResponse = new CreateContainerResponse();

        if(createContainerRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to create the container. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the container service.";
            }

            _logger.error(errorMessage, e);

            createContainerResponse.setMessage(errorMessage);

            return new ResponseEntity<>(createContainerResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Create container request is null!";

            _logger.error(errorMessage);

            createContainerResponse.setMessage(errorMessage);

            return new ResponseEntity<>(createContainerResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "retrieveContainerContentsErrorFallback")
    @RequestMapping(value = "/containers/{containerId}/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainerContentsResult> retrieveContainerContents(Authentication authentication, HttpSession session, @PathVariable String containerId, @RequestBody AssetSearchRequest assetSearchRequest){
        RetrieveContainerContentsResult retrieveContainerContentsResult = new RetrieveContainerContentsResult();
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(containerId)){
                    if(assetSearchRequest != null){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, folderServiceName);

                        if(StringUtils.isNotBlank(prefix)){
                            return restTemplate.exchange(prefix + folderServiceName + "/containers/" + containerId + "/search", HttpMethod.POST, new HttpEntity<>(assetSearchRequest, headers), RetrieveContainerContentsResult.class);
                        }
                        else{
                            throw new IllegalArgumentException("Service ID prefix is null!");
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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            retrieveContainerContentsResult.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(retrieveContainerContentsResult, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving items inside the container with ID '" + containerId + "'. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveContainerContentsResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveContainerContentsResult> retrieveContainerContentsErrorFallback(Authentication authentication, HttpSession session, @PathVariable String containerId, @RequestBody AssetSearchRequest assetSearchRequest, Throwable e){
        RetrieveContainerContentsResult retrieveContainerContentsResult = new RetrieveContainerContentsResult();

        if(StringUtils.isNotBlank(containerId)){
            if(assetSearchRequest != null){
                String errorMessage;
                if(e.getLocalizedMessage() != null){
                    errorMessage = "Unable to retrieve items in the container with ID '" + containerId + "'. " + e.getLocalizedMessage();
                }
                else{
                    errorMessage = "Unable to get response from the container service.";
                }

                _logger.error(errorMessage, e);

                retrieveContainerContentsResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Asset search request is null!";

                _logger.error(errorMessage);

                retrieveContainerContentsResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Container ID is blank!";

            _logger.error(errorMessage);

            retrieveContainerContentsResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "retrieveContainersErrorFallback")
    @RequestMapping(value = "/containers/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainersResults> retrieveContainers(Authentication authentication, HttpSession session, @RequestBody ContainerSearchRequest containerSearchRequest){
        RetrieveContainersResults retrieveContainersResults = new RetrieveContainersResults();
        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(containerSearchRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, folderServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + folderServiceName + "/containers/search", HttpMethod.POST, new HttpEntity<>(containerSearchRequest, headers), RetrieveContainersResults.class);
                    }
                    else{
                        throw new IllegalArgumentException("Service ID prefix is null!");
                    }
                }
                else{
                    String errorMessage = "Container search request is null!";

                    _logger.error(errorMessage);

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
        catch (HttpStatusCodeException httpEx){
            JsonObject responseJson = new Gson().fromJson(httpEx.getResponseBodyAsString(), JsonObject.class);
            retrieveContainersResults.setMessage(responseJson.get("message").getAsString());
            return new ResponseEntity<>(retrieveContainersResults, httpEx.getStatusCode());
        }
        catch (Exception e){
            String errorMessage = "An error occurred while searching for containers. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveContainersResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveContainersResults> retrieveContainersErrorFallback(Authentication authentication, HttpSession session, ContainerSearchRequest containerSearchRequest, Throwable e){
        RetrieveContainersResults retrieveContainersResults = new RetrieveContainersResults();

        if(containerSearchRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to retrieve containers. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the container service.";
            }

            _logger.error(errorMessage, e);

            retrieveContainersResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Container search request is null!";

            _logger.error(errorMessage);

            retrieveContainersResults.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "updateContainerErrorFallback")
    @RequestMapping(value = "/containers/{containerId}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> updateContainer(Authentication authentication, HttpSession session, @RequestBody UpdateContainerRequest updateContainerRequest, @PathVariable String containerId){
        GenericResponse genericResponse = new GenericResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(containerId)){
                    if(updateContainerRequest != null){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, folderServiceName);

                        if(StringUtils.isNotBlank(prefix)){
                            return restTemplate.exchange(prefix + folderServiceName + "/containers/" + containerId, HttpMethod.PATCH, new HttpEntity<>(updateContainerRequest, headers), GenericResponse.class);
                        }
                        else{
                            throw new IllegalArgumentException("Service ID prefix is null!");
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
                    String errorMessage = "Container ID is blank";

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
            String errorMessage = "An error occurred while updating container. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<GenericResponse> updateContainerErrorFallback(Authentication authentication, HttpSession session, UpdateContainerRequest updateContainerRequest, String containerId, Throwable e){
        if(updateContainerRequest != null){
            if(StringUtils.isNotBlank(containerId)){
                String errorMessage;
                if(e.getLocalizedMessage() != null){
                    errorMessage = "Unable to update container. " + e.getLocalizedMessage();
                }
                else{
                    errorMessage = "Unable to get response from the container service.";
                }

                _logger.error(errorMessage, e);

                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Container ID is blank!";

                _logger.error(errorMessage);

                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setMessage(errorMessage);

                return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Update container request is null!";

            _logger.error(errorMessage);

            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setMessage(errorMessage);

            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }
}