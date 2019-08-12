package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.ExternalShare;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.ExternalSharesRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.SelectedAssets;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.github.murataykanat.toybox.utilities.NotificationUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private static final String notificationServiceLoadBalancerServiceName = "toybox-notification-loadbalancer";

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
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                User user = AuthenticationUtils.getInstance().getUser(usersRepository, authentication);
                if(user != null){
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
                            HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);

                            SelectedAssets selectedAssets = new SelectedAssets();
                            selectedAssets.setSelectedAssets(externalShareRequest.getSelectedAssets());

                            HttpEntity<SelectedAssets> selectedAssetsEntity = new HttpEntity<>(selectedAssets, headers);
                            String jobServiceUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, jobServiceLoadBalancerServiceName);
                            String shareServiceUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, shareServiceLoadBalancerServiceName);

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

                                    for(Asset asset: externalShareRequest.getSelectedAssets()){
                                        // Send notification
                                        String message = "Asset '" + asset.getName() + "' is shared by '" + user.getUsername() + "'";
                                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                        sendNotificationRequest.setAsset(asset);
                                        sendNotificationRequest.setFromUser(user);
                                        sendNotificationRequest.setMessage(message);
                                        NotificationUtils.getInstance().sendNotification(sendNotificationRequest, discoveryClient, session, notificationServiceLoadBalancerServiceName);
                                    }

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
                    throw new IllegalArgumentException("User is null!");
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
