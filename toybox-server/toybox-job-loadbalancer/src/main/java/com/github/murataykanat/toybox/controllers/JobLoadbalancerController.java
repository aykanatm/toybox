package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.job.JobSearchRequest;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobResult;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobsResult;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
import com.github.murataykanat.toybox.utilities.LoadbalancerUtils;
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
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-job-loadbalancer")
@RestController
public class JobLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(JobLoadbalancerController.class);

    private static final String jobServiceName = "toybox-job-service";

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
    @HystrixCommand(fallbackMethod = "packageAssetsErrorFallback")
    @RequestMapping(value = "/jobs/package", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> packageAssets(Authentication authentication, HttpSession session, @RequestBody SelectionContext selectionContext){
        JobResponse jobResponse = new JobResponse();

        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(selectionContext != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, jobServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + jobServiceName + "/jobs/package", HttpMethod.POST, new HttpEntity<>(selectionContext, headers), JobResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Selected assets are null!";
                    _logger.error(errorMessage);

                    jobResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while initiating the package job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            _logger.debug("<< packageAssets()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<JobResponse> packageAssetsErrorFallback(Authentication authentication, HttpSession session, SelectionContext selectionContext, Throwable e){
        JobResponse jobResponse = new JobResponse();

        if(selectionContext != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable initiate the package job. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";
            _logger.error(errorMessage);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "importAssetErrorFallback")
    @RequestMapping(value = "/jobs/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> importAsset(Authentication authentication, HttpSession session, @RequestBody UploadFileLst uploadFileLst) {
        JobResponse jobResponse = new JobResponse();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(uploadFileLst != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, jobServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + jobServiceName + "/jobs/import", HttpMethod.POST, new HttpEntity<>(uploadFileLst, headers), JobResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Upload file list is null!";
                    _logger.error(errorMessage);

                    jobResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while initiating the import job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<JobResponse> importAssetErrorFallback(Authentication authentication, HttpSession session, UploadFileLst uploadFileLst, Throwable e){
        JobResponse jobResponse = new JobResponse();

        if(uploadFileLst != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable initiate the import job. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Upload file list is null!";
            _logger.error(errorMessage);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "retrieveJobsErrorFallback")
    @RequestMapping(value = "/jobs/search", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobs(Authentication authentication, HttpSession session, @RequestBody JobSearchRequest jobSearchRequest) {
        RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(jobSearchRequest != null){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, jobServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + jobServiceName + "/jobs/search", HttpMethod.POST, new HttpEntity<>(jobSearchRequest, headers), RetrieveToyboxJobsResult.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        retrieveToyboxJobsResult.setMessage(errorMessage);

                        return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Job search request is null!";
                    _logger.error(errorMessage);

                    retrieveToyboxJobsResult.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveToyboxJobsResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving jobs. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveToyboxJobsResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobsErrorFallback(Authentication authentication, HttpSession session, JobSearchRequest jobSearchRequest, Throwable e){
        RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();

        if(jobSearchRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable retrieve jobs. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);

            retrieveToyboxJobsResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job search request is null!";
            _logger.error(errorMessage);

            retrieveToyboxJobsResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "retrieveJobErrorFallback")
    @RequestMapping(value = "/jobs/{jobInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<RetrieveToyboxJobResult> retrieveJob(Authentication authentication, HttpSession session,@PathVariable String jobInstanceId){
        RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(jobInstanceId)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, jobServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + jobServiceName + "/jobs/" + jobInstanceId, HttpMethod.GET, new HttpEntity<>(headers), RetrieveToyboxJobResult.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        retrieveToyboxJobResult.setMessage(errorMessage);

                        return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Job instance id is blank!";
                    _logger.error(errorMessage);

                    retrieveToyboxJobResult.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveToyboxJobResult.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while jobs with instance ID '" + jobInstanceId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveToyboxJobResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<RetrieveToyboxJobResult> retrieveJobErrorFallback(Authentication authentication, HttpSession session, String jobInstanceId, Throwable e){
        RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable retrieve the job with the ID '" + jobInstanceId + "'. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);

            retrieveToyboxJobResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);

            retrieveToyboxJobResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "downloadJobResultErrorFallback")
    @RequestMapping(value = "/jobs/download/{jobInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadJobResult(Authentication authentication, HttpSession session, @PathVariable String jobInstanceId){
        try{
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(jobInstanceId)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, jobServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + jobServiceName + "/jobs/download/" + jobInstanceId, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Job instance id is blank!";
                    _logger.error(errorMessage);

                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading job result. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<Resource> downloadJobResultErrorFallback(Authentication authentication, HttpSession session, String jobInstanceId, Throwable e){
        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable download the result of the job with the ID '" + jobInstanceId + "'. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @LogEntryExitExecutionTime
    @HystrixCommand(fallbackMethod = "stopJobErrorFallback")
    @RequestMapping(value = "/jobs/stop/{jobInstanceId}", method = RequestMethod.POST)
    public ResponseEntity<JobResponse> stopJob(Authentication authentication, HttpSession session, @PathVariable String jobInstanceId) {
        JobResponse jobResponse = new JobResponse();
        try {
            if(AuthenticationUtils.getInstance().isSessionValid(usersRepository, authentication)){
                if(StringUtils.isNotBlank(jobInstanceId)){
                    HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
                    String prefix = LoadbalancerUtils.getInstance().getPrefix(discoveryClient, jobServiceName);

                    if(StringUtils.isNotBlank(prefix)){
                        return restTemplate.exchange(prefix + jobServiceName + "/jobs/stop/" + jobInstanceId, HttpMethod.POST, new HttpEntity<>(headers), JobResponse.class);
                    }
                    else{
                        String errorMessage = "Service ID prefix is null!";
                        _logger.error(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String errorMessage = "Job instance id is blank!";
                    _logger.error(errorMessage);

                    jobResponse.setMessage(errorMessage);

                    return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while stopping the job with instance ID '" + jobInstanceId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    public ResponseEntity<JobResponse> stopJobErrorFallback(Authentication authentication, HttpSession session, String jobInstanceId, Throwable e){
        JobResponse jobResponse = new JobResponse();

        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable stop the job with the ID '" + jobInstanceId + "'. Please check if the any of the job services are running.";
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }
}