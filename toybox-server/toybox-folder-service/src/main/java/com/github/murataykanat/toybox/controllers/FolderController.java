package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.dbo.ContainerUser;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerUsersRepository;
import com.github.murataykanat.toybox.repositories.ContainersRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.container.ContainerSearchRequest;
import com.github.murataykanat.toybox.schema.container.CreateContainerRequest;
import com.github.murataykanat.toybox.schema.container.RetrieveContainersResults;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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

    @RequestMapping(value = "/containers/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveContainersResults> retrieveContainers(Authentication authentication, @RequestBody ContainerSearchRequest containerSearchRequest){
        _logger.debug("retrieveContainers() >>");
        RetrieveContainersResults retrieveContainersResults = new RetrieveContainersResults();
        try {
            if(isSessionValid(authentication)){
                if(containerSearchRequest != null){
                    List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                    if(!usersByUsername.isEmpty()){
                        if(usersByUsername.size() == 1){
                            User user = usersByUsername.get(0);

                            int offset = containerSearchRequest.getOffset();
                            int limit = containerSearchRequest.getLimit();

                            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

                            List<Container> containersByCurrentUser;
                            List<? extends GrantedAuthority> roleAdmin = authorities.stream().filter(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")).collect(Collectors.toList());
                            if(!roleAdmin.isEmpty()){
                                _logger.debug("Retrieving the top level containers [Admin User]...");
                                containersByCurrentUser = containersRepository.getTopLevelNonDeletedContainers();
                            }
                            else{
                                _logger.debug("Retrieving the top level containers of the user '" + user.getUsername() + "'...");
                                List<Container> containersByName = containersRepository.getContainersByName(authentication.getName());
                                if(!containersByName.isEmpty()){
                                    if(containersByName.size() == 1){
                                        Container userContainer = containersByName.get(0);
                                        containersByCurrentUser = containersRepository.getNonDeletedContainersByUsernameAndParentContainerId(user.getUsername(), userContainer.getId());

                                        retrieveContainersResults.setContainerId(userContainer.getId());
                                    }
                                    else{
                                        throw new Exception("There are multiple user containers with name '" + user.getUsername() + "'!");
                                    }
                                }
                                else{
                                    throw new Exception("There is no user container with name '" + user.getUsername() + "'!");
                                }
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
                            throw new Exception("Username '" + authentication.getName() + "' is not unique!");
                        }
                    }
                    else{
                        throw new Exception("No users with username '" + authentication.getName() + " is found!");
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

    private boolean isAdminUser(Authentication authentication){
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<? extends GrantedAuthority> roleAdmin = authorities.stream().filter(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")).collect(Collectors.toList());
        if(!roleAdmin.isEmpty()){
            return true;
        }

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
        _logger.debug("Container ID: " + container.getId());
        _logger.debug("Container name: " + container.getName());
        _logger.debug("Container parent ID: " + container.getParentId());
        _logger.debug("Container created by username: " + container.getCreatedByUsername());
        _logger.debug("Container creation date: " + container.getCreationDate());

        containersRepository.insertContainer(container.getId(), container.getName(), container.getParentId(),
                container.getCreatedByUsername(), container.getCreationDate(), container.getDeleted());
    }
}
