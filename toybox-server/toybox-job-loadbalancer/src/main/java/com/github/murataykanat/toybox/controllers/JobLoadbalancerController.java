package com.github.murataykanat.toybox.controllers;

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
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@RibbonClient(name = "toybox-job-loadbalancer")
@RestController
public class JobLoadbalancerController {
    private static final Log _logger = LogFactory.getLog(JobLoadbalancerController.class);

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
    }

    @Autowired
    private RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "deleteAssetsErrorFallback")
    @RequestMapping(value = "/jobs/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> deleteAssets(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssets() >>");

        if(selectedAssets != null){
            if(session != null){
                _logger.debug("Session ID: " + session.getId());
                CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                if(token != null){
                    _logger.debug("CSRF Token: " + token.getToken());

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                    headers.set("X-XSRF-TOKEN", token.getToken());

                    _logger.debug("<< deleteAssets()");
                    return restTemplate.exchange("http://toybox-job-service/jobs/delete", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), JobResponse.class);
                }
                else{
                    String errorMessage = "CSRF token is null!";
                    _logger.error(errorMessage);

                    JobResponse jobResponse = new JobResponse();
                    jobResponse.setMessage(errorMessage);

                    _logger.debug("<< deleteAssets()");
                    return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Session is null!";
                _logger.error(errorMessage);

                JobResponse jobResponse = new JobResponse();
                jobResponse.setMessage(errorMessage);

                _logger.debug("<< deleteAssets()");
                return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        else{
            String errorMessage = "Selected assets are null!";
            _logger.error(errorMessage);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< deleteAssets()");
            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<JobResponse> deleteAssetsErrorFallback(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssetsErrorFallback() >>");

        if(selectedAssets != null){
            String errorMessage = "Unable initiate the delete job. Please check if the any of the job services are running.";
            _logger.error(errorMessage);
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
    public ResponseEntity<JobResponse> packageAssets(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("packageAssets() >>");

        if(selectedAssets != null){
            if(session != null){
                _logger.debug("Session ID: " + session.getId());
                CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                if(token != null){
                    _logger.debug("CSRF Token: " + token.getToken());

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                    headers.set("X-XSRF-TOKEN", token.getToken());

                    _logger.debug("<< packageAssets()");
                    return restTemplate.exchange("http://toybox-job-service/jobs/package", HttpMethod.POST, new HttpEntity<>(selectedAssets, headers), JobResponse.class);
                }
                else{
                    String errorMessage = "CSRF token is null!";
                    _logger.error(errorMessage);

                    JobResponse jobResponse = new JobResponse();
                    jobResponse.setMessage(errorMessage);

                    _logger.debug("<< packageAssets()");
                    return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Session is null!";
                _logger.error(errorMessage);

                JobResponse jobResponse = new JobResponse();
                jobResponse.setMessage(errorMessage);

                _logger.debug("<< packageAssets()");
                return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        else{
            String errorMessage = "Selected assets are null!";
            _logger.error(errorMessage);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< packageAssets()");
            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<JobResponse> packageAssetsErrorFallback(HttpSession session, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("packageAssetsErrorFallback() >>");

        if(selectedAssets != null){
            String errorMessage = "Unable initiate the package job. Please check if the any of the job services are running.";
            _logger.error(errorMessage);
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
    public ResponseEntity<JobResponse> importAsset(HttpSession session, @RequestBody UploadFileLst uploadFileLst) {
        _logger.debug("importAsset() >>");

        if(uploadFileLst != null){
            if(session != null){
                _logger.debug("Session ID: " + session.getId());
                CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                if(token != null){
                    _logger.debug("CSRF Token: " + token.getToken());

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                    headers.set("X-XSRF-TOKEN", token.getToken());

                    _logger.debug("<< importAsset()");
                    return restTemplate.exchange("http://toybox-job-service/jobs/import", HttpMethod.POST, new HttpEntity<>(uploadFileLst, headers), JobResponse.class);
                }
                else{
                    String errorMessage = "CSRF token is null!";
                    _logger.error(errorMessage);

                    JobResponse jobResponse = new JobResponse();
                    jobResponse.setMessage(errorMessage);

                    _logger.debug("<< importAsset()");
                    return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Session is null!";
                _logger.error(errorMessage);

                JobResponse jobResponse = new JobResponse();
                jobResponse.setMessage(errorMessage);

                _logger.debug("<< importAsset()");
                return new ResponseEntity<>(jobResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        else{
            String errorMessage = "Upload file list is null!";
            _logger.error(errorMessage);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< importAsset()");
            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<JobResponse> importAssetErrorFallback(HttpSession session, @RequestBody UploadFileLst uploadFileLst){
        _logger.debug("importAssetErrorFallback() >>");

        if(uploadFileLst != null){
            String errorMessage = "Unable initiate the import job. Please check if the any of the job services are running.";
            _logger.error(errorMessage);
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
    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobs(HttpSession session, @RequestBody JobSearchRequest jobSearchRequest) {
        _logger.debug("retrieveJobs() >>");

        if(jobSearchRequest != null){
            if(session != null){
                _logger.debug("Session ID: " + session.getId());
                CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                if(token != null){
                    _logger.debug("CSRF Token: " + token.getToken());

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                    headers.set("X-XSRF-TOKEN", token.getToken());

                    _logger.debug("<< retrieveJobs()");
                    return restTemplate.exchange("http://toybox-job-service/jobs/search", HttpMethod.POST, new HttpEntity<>(jobSearchRequest, headers), RetrieveToyboxJobsResult.class);
                }
                else{
                    String errorMessage = "CSRF token is null!";
                    _logger.error(errorMessage);

                    RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
                    retrieveToyboxJobsResult.setMessage(errorMessage);

                    _logger.debug("<< retrieveJobs()");
                    return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Session is null!";
                _logger.error(errorMessage);

                RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
                retrieveToyboxJobsResult.setMessage(errorMessage);

                _logger.debug("<< retrieveJobs()");
                return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.UNAUTHORIZED);
            }
        }
        else{
            String errorMessage = "Job search request is null!";
            _logger.error(errorMessage);

            RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
            retrieveToyboxJobsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobs()");
            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobsErrorFallback(HttpSession session, @RequestBody JobSearchRequest jobSearchRequest){
        _logger.debug("retrieveJobsErrorFallback() >>");

        if(jobSearchRequest != null){
            String errorMessage = "Unable retrieve jobs. Please check if the any of the job services are running.";
            _logger.error(errorMessage);
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
    public ResponseEntity<RetrieveToyboxJobResult> retrieveJob(HttpSession session,@PathVariable String jobInstanceId){
        _logger.debug("retrieveJob() >>");

        if(StringUtils.isNotBlank(jobInstanceId)){
            if(session != null){
                _logger.debug("Session ID: " + session.getId());
                CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                if(token != null){
                    _logger.debug("CSRF Token: " + token.getToken());

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                    headers.set("X-XSRF-TOKEN", token.getToken());

                    _logger.debug("<< retrieveJob()");
                    return restTemplate.exchange("http://toybox-job-service/jobs/" + jobInstanceId, HttpMethod.GET, new HttpEntity<>(headers), RetrieveToyboxJobResult.class);
                }
                else{
                    String errorMessage = "CSRF token is null!";
                    _logger.error(errorMessage);

                    RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
                    retrieveToyboxJobResult.setMessage(errorMessage);

                    _logger.debug("<< retrieveJob()");
                    return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Session is null!";
                _logger.error(errorMessage);

                RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
                retrieveToyboxJobResult.setMessage(errorMessage);

                _logger.debug("<< retrieveJob()");
                return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.UNAUTHORIZED);
            }
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);

            RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
            retrieveToyboxJobResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobs()");
            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<RetrieveToyboxJobResult> retrieveJobErrorFallback(HttpSession session, @PathVariable String jobInstanceId){
        _logger.debug("retrieveJobErrorFallback() >>");

        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage = "Unable retrieve the job with the ID '" + jobInstanceId + "'. Please check if the any of the job services are running.";
            _logger.error(errorMessage);
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
    public ResponseEntity<Resource> downloadJobResult(HttpSession session, @PathVariable String jobInstanceId){
        _logger.debug("downloadJobResult() >>");

        if(StringUtils.isNotBlank(jobInstanceId)){
            if(session != null){
                _logger.debug("Session ID: " + session.getId());
                CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                if(token != null){
                    _logger.debug("CSRF Token: " + token.getToken());

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
                    headers.set("X-XSRF-TOKEN", token.getToken());

                    _logger.debug("<< retrieveJob()");
                    return restTemplate.exchange("http://toybox-job-service/jobs/download/" + jobInstanceId, HttpMethod.GET, new HttpEntity<>(headers), Resource.class);
                }
                else{
                    String errorMessage = "CSRF token is null!";
                    _logger.error(errorMessage);

                    _logger.debug("<< retrieveJob()");
                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "Session is null!";
                _logger.error(errorMessage);

                _logger.debug("<< retrieveJob()");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        else{
            String errorMessage = "Job instance id is blank!";
            _logger.error(errorMessage);

            _logger.debug("<< retrieveJobs()");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<Resource> downloadJobResultErrorFallback(HttpSession session, @PathVariable String jobInstanceId){
        _logger.debug("downloadJobResultErrorFallback() >>");

        if(StringUtils.isNotBlank(jobInstanceId)){
            String errorMessage = "Unable download the result of the job with the ID '" + jobInstanceId + "'. Please check if the any of the job services are running.";
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
}
