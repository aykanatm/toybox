package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.dbo.ExternalShare;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.ExternalSharesRepository;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareRequest;
import com.github.murataykanat.toybox.schema.share.ExternalShareResponse;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import com.github.murataykanat.toybox.utilities.NotificationUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RefreshScope
@RestController
public class ShareController {
    private static final Log _logger = LogFactory.getLog(ShareController.class);

    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private NotificationUtils notificationUtils;

    @Autowired
    private ExternalSharesRepository externalSharesRepository;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/download/{externalShareId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadExternalShare(@PathVariable String externalShareId){
        try{
            if(StringUtils.isNotBlank(externalShareId)){
                List<ExternalShare> externalSharesById = externalSharesRepository.getExternalSharesById(externalShareId);
                if(!externalSharesById.isEmpty()){
                    if(externalSharesById.size() == 1){
                        ExternalShare externalShare = externalSharesById.get(0);

                        boolean canDownload = false;

                        Date expirationDate = externalShare.getExpirationDate();
                        if(expirationDate != null){
                            Calendar cal = Calendar.getInstance();
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            Date today = cal.getTime();

                            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                            _logger.debug("Expiration date: " + formatter.format(expirationDate));
                            _logger.debug("Today: " + formatter.format(today));

                            if(today.before(expirationDate) && (externalShare.getMaxNumberOfHits() == -1 || externalShare.getMaxNumberOfHits() > 0)){
                                canDownload = true;
                            }
                        }
                        else{
                            throw new IllegalArgumentException("Expiration date is null!");
                        }

                        if(canDownload){
                            String downloadFilePath =  exportStagingPath + File.separator + externalShare.getJobId() + File.separator + "Download.zip";

                            File archiveFile = new File(downloadFilePath);
                            if(archiveFile.exists()){
                                InputStreamResource resource = new InputStreamResource(new FileInputStream(archiveFile));

                                HttpHeaders headers = new HttpHeaders();
                                headers.set("Content-disposition", "attachment; filename=Download.zip");
                                return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                            }
                            else{
                                throw new IOException("File path '" + downloadFilePath + "' is not valid!");
                            }
                        }
                        else{
                            String errorMessage = "Either the external share is expired or the maximum number of uses exceeded the set amount.!";
                            _logger.error(errorMessage);

                            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        throw new Exception("There are multiple external shares with ID '" + externalShareId + "'!");
                    }
                }
                else{
                    String errorMessage = "External share ID is not found! ";
                    _logger.error(errorMessage);

                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            }
            else{
                String errorMessage = "External share ID is blank! ";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading the shared assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/share/external", method = RequestMethod.POST)
    public ResponseEntity<ExternalShareResponse> createExternalShare(Authentication authentication, HttpSession session, @RequestBody ExternalShareRequest externalShareRequest) {
        ExternalShareResponse externalShareResponse = new ExternalShareResponse();
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    if(externalShareRequest != null){
                        SelectionContext selectionContext = externalShareRequest.getSelectionContext();
                        if(selectionContext != null){
                            String username = authentication.getName();
                            Date expirationDate = externalShareRequest.getExpirationDate();
                            int maxNumberOfHits = externalShareRequest.getMaxNumberOfHits();
                            String notifyWhenDownloaded = externalShareRequest.getNotifyWhenDownloaded() ? "Y" : "N";

                            if(expirationDate == null){
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
                                expirationDate = simpleDateFormat.parse("12/31/9999 23:59:59");
                            }

                            RestTemplate restTemplate = new RestTemplate();
                            HttpHeaders headers = authenticationUtils.getHeaders(session);

                            HttpEntity<SelectionContext> selectionContextEntity = new HttpEntity<>(selectionContext, headers);
                            String jobServiceUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.JOB_SERVICE_LOAD_BALANCER_SERVICE_NAME);
                            String shareServiceUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.SHARE_LOAD_BALANCER_SERVICE_NAME);

                            ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/package", selectionContextEntity, JobResponse.class);
                            if(jobResponseResponseEntity != null){
                                JobResponse jobResponse = jobResponseResponseEntity.getBody();
                                if(jobResponse != null){
                                    _logger.debug("Job response message: " + jobResponse.getMessage());
                                    _logger.debug("Job ID: " + jobResponse.getJobId());

                                    String externalShareId = generateExternalShareId();

                                    externalSharesRepository.insertExternalShare(externalShareId, username, jobResponse.getJobId(), expirationDate, maxNumberOfHits, notifyWhenDownloaded);

                                    externalShareResponse.setMessage("External share successfully generated.");
                                    externalShareResponse.setUrl(shareServiceUrl + "/share/download/" + externalShareId);

                                    List<Asset> selectedAssets = externalShareRequest.getSelectionContext().getSelectedAssets();
                                    List<Container> selectedContainers = externalShareRequest.getSelectionContext().getSelectedContainers();

                                    if(selectedAssets != null && !selectedAssets.isEmpty()){
                                        for(Asset asset: selectedAssets){
                                            // Send notification
                                            String message = "Asset '" + asset.getName() + "' is shared externally by '" + user.getUsername() + "'";
                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                                            sendNotificationRequest.setIsAsset(true);
                                            sendNotificationRequest.setId(asset.getId());
                                            sendNotificationRequest.setFromUser(user);
                                            sendNotificationRequest.setMessage(message);
                                            notificationUtils.sendNotification(sendNotificationRequest, session);
                                        }
                                    }

                                    // TODO: Make a notification for containers
//                                    if(selectedContainers != null && !selectedContainers.isEmpty()){
//                                        for(Container container: selectedContainers){
//                                            // Send notification
//                                            String message = "Folder '" + container.getName() + "' is shared externally by '" + user.getUsername() + "'";
//                                            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
//                                            sendNotificationRequest.setAsset(container);
//                                            sendNotificationRequest.setFromUser(user);
//                                            sendNotificationRequest.setMessage(message);
//                                            NotificationUtils.getInstance().sendNotification(sendNotificationRequest, discoveryClient, session, notificationServiceLoadBalancerServiceName);
//                                        }
//                                    }

                                    return new ResponseEntity<>(externalShareResponse, HttpStatus.CREATED);
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
                            throw new IllegalArgumentException("Selection context is null!");
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
