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
import com.github.murataykanat.toybox.schema.container.RetrieveContainersResults;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.utilities.SortUtils;
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
                                return new ResponseEntity<>(retrieveContainersResults, HttpStatus.NO_CONTENT);
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
}
