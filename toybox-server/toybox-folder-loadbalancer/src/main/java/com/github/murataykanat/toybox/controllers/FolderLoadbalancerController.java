package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.container.ContainerSearchRequest;
import com.github.murataykanat.toybox.schema.container.CreateContainerRequest;
import com.github.murataykanat.toybox.schema.container.RetrieveContainerContentsResult;
import com.github.murataykanat.toybox.schema.container.RetrieveContainersResults;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

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

    @HystrixCommand(fallbackMethod = "createContainerErrorFallback")
    @RequestMapping(value = "/containers", method = RequestMethod.POST)
    public ResponseEntity<GenericResponse> createContainer(Authentication authentication,  HttpSession session, @RequestBody CreateContainerRequest createContainerRequest){
        _logger.debug("createContainer() >>");
        GenericResponse genericResponse = new GenericResponse();
        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(createContainerRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, folderServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< createContainer()");
                        return restTemplate.exchange(prefix + folderServiceName + "/containers", HttpMethod.POST, new HttpEntity<>(createContainerRequest, headers), GenericResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";

                        _logger.error(errorMessage);

                        genericResponse.setMessage(errorMessage);

                        _logger.debug("<< createContainer()");
                        return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Create container request is null!";

                    _logger.error(errorMessage);

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

    public ResponseEntity<GenericResponse> createContainerErrorFallback(Authentication authentication, HttpSession session, CreateContainerRequest createContainerRequest, Throwable e){
        _logger.debug("createContainerErrorFallback() >>");
        GenericResponse genericResponse = new GenericResponse();

        if(createContainerRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable to create the container. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the container service.";
            }

            _logger.error(errorMessage, e);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< createContainerErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Create container request is null!";

            _logger.error(errorMessage);

            genericResponse.setMessage(errorMessage);

            _logger.debug("<< createContainerErrorFallback()");
            return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "retrieveContainerContentsErrorFallback")
    @RequestMapping(value = "/containers/{containerId}/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainerContentsResult> retrieveContainerContents(Authentication authentication, HttpSession session, @PathVariable String containerId, @RequestBody AssetSearchRequest assetSearchRequest){
        _logger.debug("retrieveContainerContents()");
        RetrieveContainerContentsResult retrieveContainerContentsResult = new RetrieveContainerContentsResult();
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(containerId)){
                    if(assetSearchRequest != null){
                        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                        String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, folderServiceName);

                        if(StringUtils.isNotBlank(prefix)){
                            _logger.debug("<< retrieveContainerContents()");
                            return restTemplate.exchange(prefix + folderServiceName + "/containers/" + containerId + "/search", HttpMethod.POST, new HttpEntity<>(assetSearchRequest, headers), RetrieveContainerContentsResult.class);
                        }
                        else{
                            String errorMessage = "Service ID prefix is null!";

                            _logger.error(errorMessage);

                            retrieveContainerContentsResult.setMessage(errorMessage);

                            _logger.debug("<< retrieveContainerContents()");
                            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.INTERNAL_SERVER_ERROR);
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

    public ResponseEntity<RetrieveContainerContentsResult> retrieveContainerContentsErrorFallback(Authentication authentication, HttpSession session, @PathVariable String containerId, @RequestBody AssetSearchRequest assetSearchRequest, Throwable e){
        _logger.debug("retrieveContainerContentsErrorFallback() >>");
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

                _logger.debug("<< retrieveContainerContentsErrorFallback()");
                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else{
                String errorMessage = "Asset search request is null!";

                _logger.error(errorMessage);

                retrieveContainerContentsResult.setMessage(errorMessage);

                _logger.debug("<< retrieveContainerContentsErrorFallback()");
                return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            String errorMessage = "Container ID is blank!";

            _logger.error(errorMessage);

            retrieveContainerContentsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveContainerContentsErrorFallback()");
            return new ResponseEntity<>(retrieveContainerContentsResult, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "retrieveContainersErrorFallback")
    @RequestMapping(value = "/containers/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainersResults> retrieveContainers(Authentication authentication, HttpSession session, @RequestBody ContainerSearchRequest containerSearchRequest){
        _logger.debug("retrieveContainers() >>");
        RetrieveContainersResults retrieveContainersResults = new RetrieveContainersResults();
        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(containerSearchRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, folderServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        _logger.debug("<< retrieveContainers()");
                        return restTemplate.exchange(prefix + folderServiceName + "/containers/search", HttpMethod.POST, new HttpEntity<>(containerSearchRequest, headers), RetrieveContainersResults.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";

                        _logger.error(errorMessage);

                        retrieveContainersResults.setMessage(errorMessage);

                        _logger.debug("<< retrieveContainers()");
                        return new ResponseEntity<>(retrieveContainersResults, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Container search request is null!";

                    _logger.error(errorMessage);

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
            String errorMessage = "An error occurred while searching for containers. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveContainersResults.setMessage(errorMessage);

            _logger.debug("<< retrieveContainers()");
            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<RetrieveContainersResults> retrieveContainersErrorFallback(Authentication authentication, HttpSession session, ContainerSearchRequest containerSearchRequest, Throwable e){
        _logger.debug("retrieveContainersErrorFallback() >>");
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

            _logger.debug("<< retrieveContainersErrorFallback()");
            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Container search request is null!";

            _logger.error(errorMessage);

            retrieveContainersResults.setMessage(errorMessage);

            _logger.debug("<< retrieveContainersErrorFallback()");
            return new ResponseEntity<>(retrieveContainersResults, HttpStatus.BAD_REQUEST);
        }
    }
}
