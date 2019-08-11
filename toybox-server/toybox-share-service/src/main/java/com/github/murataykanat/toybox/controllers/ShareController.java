package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.ExternalShare;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.ExternalSharesRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.SelectedAssets;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
import org.apache.commons.lang.RandomStringUtils;
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
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RefreshScope
@RestController
public class ShareController {
    private static final Log _logger = LogFactory.getLog(ShareController.class);

    private static final String jobServiceLoadBalancerServiceName = "toybox-job-loadbalancer";
    private static final String shareServiceLoadBalancerServiceName = "toybox-share-loadbalancer";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ExternalSharesRepository externalSharesRepository;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/external", method = RequestMethod.POST)
    public ResponseEntity<ExternalShareResponse> createExternalShare(Authentication authentication, HttpSession session, @RequestBody ExternalShareRequest externalShareRequest) {
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();
        try{
            if(isSessionValid(authentication)){
                if(externalShareRequest != null){
                    if(!externalShareRequest.getSelectedAssets().isEmpty()){
                        String username = authentication.getName();
                        Date expirationDate = externalShareRequest.getExpirationDate();
                        int maxNumberOfHits = externalShareRequest.getMaxNumberOfHits();
                        String notifyWhenDownloaded = externalShareRequest.getNotifyWhenDownloaded() ? "Y" : "N";

                        if(expirationDate == null){
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
                            expirationDate = simpleDateFormat.parse("12/31/9999 23:59:59");
                        }

                        RestTemplate restTemplate = new RestTemplate();
                        HttpHeaders headers = getHeaders(session);

                        SelectedAssets selectedAssets = new SelectedAssets();
                        selectedAssets.setSelectedAssets(externalShareRequest.getSelectedAssets());

                        HttpEntity<SelectedAssets> selectedAssetsEntity = new HttpEntity<>(selectedAssets, headers);
                        String jobServiceUrl = getLoadbalancerUrl(jobServiceLoadBalancerServiceName);
                        String shareServiceUrl = getLoadbalancerUrl(shareServiceLoadBalancerServiceName);

                        ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/package", selectedAssetsEntity, JobResponse.class);
                        if(jobResponseResponseEntity != null){
                            JobResponse jobResponse = jobResponseResponseEntity.getBody();
                            if(jobResponse != null){
                                _logger.debug("Job response message: " + jobResponse.getMessage());
                                _logger.debug("Job ID: " + jobResponse.getJobId());

                                String externalShareId = generateExternalShareId();

                                externalSharesRepository.insertExternalShare(externalShareId, username, jobResponse.getJobId(), expirationDate, maxNumberOfHits, notifyWhenDownloaded);

                                externalShareResponse.setMessage("External share successfully generated.");
                                externalShareResponse.setUrl(shareServiceUrl + "/share/external?id=" + externalShareId);

                                return new ResponseEntity<>(externalShareResponse, HttpStatus.OK);
                            }
                            else{
                                throw new IllegalArgumentException("Job response is null!");
                            }
                        }
                        else{
                            throw new IllegalArgumentException("Job response entity is null!");
                        }
                    }
                    else{
                        String warningMessage = "No assets were selected!";
                        _logger.warn(warningMessage);

                        externalShareResponse.setMessage(warningMessage);

                        return new ResponseEntity<>(externalShareResponse, HttpStatus.NOT_FOUND);
                    }
                }
                else{
                    String errorMessage = "External share request is null!";
                    _logger.error(errorMessage);

                    externalShareResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(externalShareResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                externalShareResponse.setMessage(errorMessage);

                return new ResponseEntity<>(externalShareResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while sharing assets externally. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            externalShareResponse.setMessage(errorMessage);

            return new ResponseEntity<>(externalShareResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    private HttpHeaders getHeaders(HttpSession session) {
        HttpHeaders headers = new HttpHeaders();

        _logger.debug("Session ID: " + session.getId());
        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
        if(token != null){
            _logger.debug("CSRF Token: " + token.getToken());
            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
            headers.set("X-XSRF-TOKEN", token.getToken());

            return headers;
        }
        else{
            throw new IllegalArgumentException("CSRF token is null!");
        }
    }

    @LogEntryExitExecutionTime
    private String getLoadbalancerUrl(String loadbalancerServiceName) throws Exception {
        List<ServiceInstance> instances = discoveryClient.getInstances(loadbalancerServiceName);
        if(!instances.isEmpty()){
            ServiceInstance serviceInstance = instances.get(0);
            _logger.debug("Load balancer URL: " + serviceInstance.getUri().toString());
            return serviceInstance.getUri().toString();
        }
        else{
            throw new Exception("There is no load balancer instance with name '" + loadbalancerServiceName + "'.");
        }
    }

    @LogEntryExitExecutionTime
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

    @LogEntryExitExecutionTime
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

    @LogEntryExitExecutionTime
    private String generateExternalShareId(){
        String externalShareId = RandomStringUtils.randomAlphanumeric(40);
        if(isExternalShareIdValid(externalShareId)){
            return externalShareId;
        }
        return generateExternalShareId();
    }

    @LogEntryExitExecutionTime
    private boolean isExternalShareIdValid(String externalShareId){
        List<ExternalShare> externalSharesById = externalSharesRepository.getExternalSharesById(externalShareId);
        if(externalSharesById.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }
}
