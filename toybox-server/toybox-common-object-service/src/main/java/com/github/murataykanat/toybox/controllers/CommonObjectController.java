package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.JobUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;

@RefreshScope
@RestController
public class CommonObjectController {
    private static final Log _logger = LogFactory.getLog(CommonObjectController.class);

    private static final String jobServiceLoadBalancerServiceName = "toybox-job-loadbalancer";
    private static final String assetServiceLoadBalancerServiceName = "toybox-asset-loadbalancer";
    private static final String notificationServiceLoadBalancerServiceName = "toybox-notification-loadbalancer";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private AssetUserRepository assetUserRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/common-objects/download", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadObjects(Authentication authentication, HttpSession session, @RequestBody SelectionContext selectionContext){
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null){
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);

                    HttpEntity<SelectionContext> selectionContextHttpEntity = new HttpEntity<>(selectionContext, headers);

                    String jobServiceUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, jobServiceLoadBalancerServiceName);

                    ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/package", selectionContextHttpEntity, JobResponse.class);
                    if(jobResponseResponseEntity != null){
                        _logger.debug(jobResponseResponseEntity);
                        JobResponse jobResponse = jobResponseResponseEntity.getBody();
                        if(jobResponse != null){
                            _logger.debug("Job response message: " + jobResponse.getMessage());
                            _logger.debug("Job ID: " + jobResponse.getJobId());
                            File archiveFile = JobUtils.getInstance().getArchiveFile(jobResponse.getJobId(), headers, jobServiceUrl, exportStagingPath);
                            if(archiveFile != null && archiveFile.exists()){
                                InputStreamResource resource = new InputStreamResource(new FileInputStream(archiveFile));
                                return new ResponseEntity<>(resource, HttpStatus.OK);
                            }
                            else{
                                if(archiveFile != null){
                                    throw new IOException("File '" + archiveFile.getAbsolutePath() + "' does not exist!");
                                }
                                throw new InvalidObjectException("Archive file is null!");
                            }
                        }
                        else{
                            throw new InvalidObjectException("Job response is null!");
                        }
                    }
                    else{
                        throw new InvalidObjectException("Job response entity is null!");
                    }
                }
                else{
                    String errorMessage = "Selected assets parameter is null!";
                    _logger.error(errorMessage);

                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading selected assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}