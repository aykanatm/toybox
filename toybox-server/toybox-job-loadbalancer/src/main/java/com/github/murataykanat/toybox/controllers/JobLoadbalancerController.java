package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.SelectedAssets;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.job.JobSearchRequest;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobResult;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobsResult;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
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
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

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

    @HystrixCommand(fallbackMethod = "deleteAssetsErrorFallback")
    @RequestMapping(value = "/jobs/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> deleteAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssets() >>");
        try{
            JobResponse jobResponse = new JobResponse();

            if(selectedAssets != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
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
                                return restTemplate.exchange(prefix + jobServiceName + "/jobs/delete", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), JobResponse.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                jobResponse.setMessage(errorMessage);

                                _logger.debug("<< deleteAssets()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String errorMessage = "CSRF token is null!";
                            _logger.error(errorMessage);

                            jobResponse.setMessage(errorMessage);

                            _logger.debug("<< deleteAssets()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        _logger.debug("<< deleteAssets()");
                        return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    jobResponse.setMessage(errorMessage);

                    _logger.debug("<< deleteAssets()");
                    return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                _logger.debug("<< deleteAssets()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while initiating the delete job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< deleteAssets()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<JobResponse> deleteAssetsErrorFallback(Authentication authentication, HttpSession session, SelectedAssets selectedAssets, Throwable e){
        _logger.debug("deleteAssetsErrorFallback() >>");

        if(selectedAssets != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable initiate the delete job. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }
            _logger.error(errorMessage, e);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< deleteAssetsErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";
            _logger.error(errorMessage);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< deleteAssetsErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "packageAssetsErrorFallback")
    @RequestMapping(value = "/jobs/package", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> packageAssets(Authentication authentication, HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("packageAssets() >>");
        try {
            JobResponse jobResponse = new JobResponse();

            if(selectedAssets != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< packageAssets()");
                                return restTemplate.exchange(prefix + jobServiceName + "/jobs/package", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), JobResponse.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                jobResponse.setMessage(errorMessage);

                                _logger.debug("<< packageAssets()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String errorMessage = "CSRF token is null!";
                            _logger.error(errorMessage);

                            jobResponse.setMessage(errorMessage);

                            _logger.debug("<< packageAssets()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        _logger.debug("<< packageAssets()");
                        return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    jobResponse.setMessage(errorMessage);

                    _logger.debug("<< packageAssets()");
                    return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Selected assets are null!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                _logger.debug("<< packageAssets()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while initiating the package job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< packageAssets()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<JobResponse> packageAssetsErrorFallback(Authentication authentication, HttpSession session, SelectedAssets selectedAssets, Throwable e){
        _logger.debug("packageAssetsErrorFallback() >>");

        if(selectedAssets != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable initiate the package job. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< packageAssetsErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Selected assets are null!";
            _logger.error(errorMessage);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< packageAssetsErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "importAssetErrorFallback")
    @RequestMapping(value = "/jobs/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> importAsset(Authentication authentication, HttpSession session, @RequestBody UploadFileLst uploadFileLst) {
        _logger.debug("importAsset() >>");
        try{
            JobResponse jobResponse = new JobResponse();

            if(uploadFileLst != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< importAsset()");
                                return restTemplate.exchange(prefix + jobServiceName + "/jobs/import", HttpMethod.POST, new HttpEntity<>(uploadFileLst, headers), JobResponse.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                jobResponse.setMessage(errorMessage);

                                _logger.debug("<< importAsset()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String errorMessage = "CSRF token is null!";
                            _logger.error(errorMessage);

                            jobResponse.setMessage(errorMessage);

                            _logger.debug("<< importAsset()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        _logger.debug("<< importAsset()");
                        return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    jobResponse.setMessage(errorMessage);

                    _logger.debug("<< importAsset()");
                    return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Upload file list is null!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                _logger.debug("<< importAsset()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while initiating the import job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< importAsset()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<JobResponse> importAssetErrorFallback(Authentication authentication, HttpSession session, UploadFileLst uploadFileLst, Throwable e){
        _logger.debug("importAssetErrorFallback() >>");

        if(uploadFileLst != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable initiate the import job. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< importAssetErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Upload file list is null!";
            _logger.error(errorMessage);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< importAssetErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "retrieveJobsErrorFallback")
    @RequestMapping(value = "/jobs/search", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobs(Authentication authentication, HttpSession session, @RequestBody JobSearchRequest jobSearchRequest) {
        _logger.debug("retrieveJobs() >>");
        try{
            RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();

            if(jobSearchRequest != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< retrieveJobs()");
                                return restTemplate.exchange(prefix + jobServiceName + "/jobs/search", HttpMethod.POST, new HttpEntity<>(jobSearchRequest, headers), RetrieveToyboxJobsResult.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                retrieveToyboxJobsResult.setMessage(errorMessage);

                                _logger.debug("<< importAsset()");
                                return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String errorMessage = "CSRF token is null!";
                            _logger.error(errorMessage);

                            retrieveToyboxJobsResult.setMessage(errorMessage);

                            _logger.debug("<< retrieveJobs()");
                            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.UNAUTHORIZED);
                        }

                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        retrieveToyboxJobsResult.setMessage(errorMessage);

                        _logger.debug("<< retrieveJobs()");
                        return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    retrieveToyboxJobsResult.setMessage(errorMessage);

                    _logger.debug("<< retrieveJobs()");
                    return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Job search request is null!";
                _logger.error(errorMessage);

                retrieveToyboxJobsResult.setMessage(errorMessage);

                _logger.debug("<< retrieveJobs()");
                return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving jobs. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
            retrieveToyboxJobsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobs()");
            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobsErrorFallback(Authentication authentication, HttpSession session, JobSearchRequest jobSearchRequest, Throwable e){
        _logger.debug("retrieveJobsErrorFallback() >>");

        if(jobSearchRequest != null){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable retrieve jobs. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);
            RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
            retrieveToyboxJobsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobsErrorFallback()");
            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job search request is null!";
            _logger.error(errorMessage);
            RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
            retrieveToyboxJobsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobsErrorFallback()");
            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "retrieveJobErrorFallback")
    @RequestMapping(value = "/jobs/{jobInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<RetrieveToyboxJobResult> retrieveJob(Authentication authentication, HttpSession session,@PathVariable String jobInstanceId){
        _logger.debug("retrieveJob() >>");
        try{
            RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

            if(StringUtils.isNotBlank(jobInstanceId)){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< retrieveJob()");
                                return restTemplate.exchange(prefix + jobServiceName + "/jobs/" + jobInstanceId, HttpMethod.GET, new HttpEntity<>(headers), RetrieveToyboxJobResult.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                retrieveToyboxJobResult.setMessage(errorMessage);

                                _logger.debug("<< retrieveJob()");
                                return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
                            }

                        }
                        else{
                            String errorMessage = "CSRF token is null!";
                            _logger.error(errorMessage);

                            retrieveToyboxJobResult.setMessage(errorMessage);

                            _logger.debug("<< retrieveJob()");
                            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        retrieveToyboxJobResult.setMessage(errorMessage);

                        _logger.debug("<< retrieveJob()");
                        return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    retrieveToyboxJobResult.setMessage(errorMessage);

                    _logger.debug("<< retrieveJob()");
                    return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Job instance id is blank!";
                _logger.error(errorMessage);

                retrieveToyboxJobResult.setMessage(errorMessage);

                _logger.debug("<< retrieveJob()");
                return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while jobs with instance ID '" + jobInstanceId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
            retrieveToyboxJobResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJob()");
            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<RetrieveToyboxJobResult> retrieveJobErrorFallback(Authentication authentication, HttpSession session, String jobInstanceId, Throwable e){
        _logger.debug("retrieveJobErrorFallback() >>");

        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable retrieve the job with the ID '" + jobInstanceId + "'. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);
            RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
            retrieveToyboxJobResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobErrorFallback()");
            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);
            RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
            retrieveToyboxJobResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobErrorFallback()");
            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "downloadJobResultErrorFallback")
    @RequestMapping(value = "/jobs/download/{jobInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadJobResult(Authentication authentication, HttpSession session, @PathVariable String jobInstanceId){
        _logger.debug("downloadJobResult() >>");
        try{
            if(StringUtils.isNotBlank(jobInstanceId)){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< downloadJobResult()");
                                return restTemplate.exchange(prefix + jobServiceName + "/jobs/download/" + jobInstanceId, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                _logger.debug("<< downloadJobResult()");
                                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String errorMessage = "CSRF token is null!";
                            _logger.error(errorMessage);

                            _logger.debug("<< downloadJobResult()");
                            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        _logger.debug("<< downloadJobResult()");
                        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    _logger.debug("<< downloadJobResult()");
                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Job instance id is blank!";
                _logger.error(errorMessage);

                _logger.debug("<< downloadJobResult()");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while downloading job result. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< downloadJobResult()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Resource> downloadJobResultErrorFallback(Authentication authentication, HttpSession session, String jobInstanceId, Throwable e){
        _logger.debug("downloadJobResultErrorFallback() >>");

        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable download the result of the job with the ID '" + jobInstanceId + "'. " + e.getLocalizedMessage();
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage);

            _logger.debug("<< downloadJobResultErrorFallback()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);

            _logger.debug("<< downloadJobResultErrorFallback()");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @HystrixCommand(fallbackMethod = "stopJobErrorFallback")
    @RequestMapping(value = "/jobs/stop/{jobInstanceId}", method = RequestMethod.POST)
    public ResponseEntity<JobResponse> stopJob(Authentication authentication, HttpSession session, @PathVariable String jobInstanceId) {
        _logger.debug("stopJob() >>");
        try {
            JobResponse jobResponse = new JobResponse();

            if(StringUtils.isNotBlank(jobInstanceId)){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        _logger.debug("Session ID: " + session.getId());
                        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                        if(token != null){
                            _logger.debug("CSRF Token: " + token.getToken());

                            HttpHeaders headers = new HttpHeaders();
                            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                            headers.set("X-XSRF-TOKEN", token.getToken());

                            String prefix = getPrefix();
                            if(StringUtils.isNotBlank(prefix)){
                                _logger.debug("<< stopJob()");
                                return restTemplate.exchange(prefix + jobServiceName + "/jobs/stop/" + jobInstanceId, HttpMethod.POST, new HttpEntity<>(headers), JobResponse.class);
                            }
                            else{
                                String errorMessage = "Service ID prefix is null!";
                                _logger.error(errorMessage);

                                jobResponse.setMessage(errorMessage);

                                _logger.debug("<< stopJob()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
                            }

                        }
                        else{
                            String errorMessage = "CSRF token is null!";
                            _logger.error(errorMessage);

                            jobResponse.setMessage(errorMessage);

                            _logger.debug("<< stopJob()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                        }
                    }
                    else{
                        String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                        _logger.debug(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        _logger.debug("<< stopJob()");
                        return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                    }
                }
                else{
                    String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                    _logger.debug(errorMessage);

                    jobResponse.setMessage(errorMessage);

                    _logger.debug("<< stopJob()");
                    return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Job instance id is blank!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                _logger.debug("<< stopJob()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while stopping the job with instance ID '" + jobInstanceId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< retrieveJob()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<JobResponse> stopJobErrorFallback(Authentication authentication, HttpSession session, String jobInstanceId, Throwable e){
        _logger.debug("stopJobErrorFallback() >>");

        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage;
            if(e.getLocalizedMessage() != null){
                errorMessage = "Unable stop the job with the ID '" + jobInstanceId + "'. Please check if the any of the job services are running.";
            }
            else{
                errorMessage = "Unable to get response from the job service.";
            }

            _logger.error(errorMessage, e);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< stopJobErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< stopJobErrorFallback()");
            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    private String getPrefix() throws Exception {
        _logger.debug("getPrefix() >>");
        List<ServiceInstance> instances = discoveryClient.getInstances(jobServiceName);
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
                String errorMessage = "Not all job services have the same transfer protocol!";
                _logger.error(errorMessage);

                throw new Exception(errorMessage);

            }
        }
        else{
            String errorMessage = "No job services are running!";
            _logger.error(errorMessage);

            throw new Exception(errorMessage);
        }
    }
}
