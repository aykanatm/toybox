package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.batch.utils.Constants;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.models.job.ToyboxJob;
import com.github.murataykanat.toybox.models.job.ToyboxJobStep;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.JobStepsRepository;
import com.github.murataykanat.toybox.repositories.JobsRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.asset.SelectedAssets;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.job.*;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import com.github.murataykanat.toybox.utilities.SortUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobExecutionNotStoppedException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class JobController {
    private static final Log _logger = LogFactory.getLog(JobController.class);

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private JobOperator jobOperator;
    @Autowired
    private Job importJob;
    @Autowired
    private Job packagingJob;
    @Autowired
    private Job deleteJob;

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private JobsRepository jobsRepository;
    @Autowired
    JobStepsRepository jobStepsRepository;
    @Autowired
    private UsersRepository usersRepository;

    @RequestMapping(value = "/jobs/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> deleteAssets(Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("deleteAssets() >>");
        try{
            JobResponse jobResponse = new JobResponse();

            if(selectedAssets != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        List<String> selectedAssetIds = selectedAssets.getSelectedAssets().stream().map(asset -> asset.getId()).collect(Collectors.toList());
                        if(!selectedAssetIds.isEmpty()){
                            List<Asset> allAssets = assetsRepository.getNonDeletedAssets();

                            List<Asset> assets = allAssets.stream().filter(asset -> selectedAssetIds.contains(asset.getId())).collect(Collectors.toList());

                            if(!assets.isEmpty()){
                                JobParametersBuilder builder = new JobParametersBuilder();

                                for(int i = 0; i < assets.size(); i++){
                                    Asset asset = assets.get(i);
                                    builder.addString(Constants.JOB_PARAM_DELETE_ASSET_ID + "_" + i, asset.getId());
                                }

                                builder.addString(Constants.JOB_PARAM_USERNAME, authentication.getName());
                                builder.addString(Constants.JOB_PARAM_SYSTEM_MILLIS, String.valueOf(System.currentTimeMillis()));

                                _logger.debug("Launching job [" + deleteJob.getName() + "]...");
                                JobExecution jobExecution = jobLauncher.run(deleteJob, builder.toJobParameters());
                                jobExecution.getExecutionContext().put("jobId", jobExecution.getJobId());

                                jobResponse.setJobId(jobExecution.getJobId());
                                jobResponse.setMessage("Packaging job started.");

                                _logger.debug("<< deleteAssets()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
                            }
                            else{
                                String message = "No assets were found in the system with the requested IDs.";
                                jobResponse.setMessage(message);

                                _logger.debug("<< deleteAssets()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.NO_CONTENT);
                            }
                        }
                        else{
                            String message = "No assets were found in the request.";
                            jobResponse.setMessage(message);

                            _logger.debug("<< deleteAssets()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.NOT_FOUND);
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
                String message = "Selected assets are null!";
                jobResponse.setMessage(message);

                _logger.debug("<< deleteAssets()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while deleting a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< deleteAssets()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/jobs/package", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> packageAssets(Authentication authentication, @RequestBody SelectedAssets selectedAssets){
        _logger.debug("packageAssets() >>");
        try{
            JobResponse jobResponse = new JobResponse();

            if(selectedAssets != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        List<String> selectedAssetIds = selectedAssets.getSelectedAssets().stream().map(asset -> asset.getId()).collect(Collectors.toList());

                        if(!selectedAssetIds.isEmpty()){
                            List<Asset> allAssets = assetsRepository.getNonDeletedAssets();

                            List<Asset> assets = allAssets.stream().filter(asset -> selectedAssetIds.contains(asset.getId())).collect(Collectors.toList());

                            if(!assets.isEmpty()){
                                JobParametersBuilder builder = new JobParametersBuilder();

                                for(int i = 0; i < assets.size(); i++){
                                    Asset asset = assets.get(i);
                                    builder.addString(Constants.JOB_PARAM_PACKAGING_FILE + "_" + i, asset.getPath());
                                }
                                builder.addString(Constants.JOB_PARAM_USERNAME, authentication.getName());
                                builder.addString(Constants.JOB_PARAM_SYSTEM_MILLIS, String.valueOf(System.currentTimeMillis()));

                                _logger.debug("Launching job [" + packagingJob.getName() + "]...");
                                JobExecution jobExecution = jobLauncher.run(packagingJob, builder.toJobParameters());
                                jobExecution.getExecutionContext().put("jobId", jobExecution.getJobId());

                                jobResponse.setJobId(jobExecution.getJobId());
                                jobResponse.setMessage("Packaging job started.");

                                _logger.debug("<< packageAssets()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
                            }
                            else{
                                String message = "No assets were found in the system with the requested IDs.";
                                jobResponse.setMessage(message);

                                _logger.debug("<< packageAssets()");
                                return new ResponseEntity<>(jobResponse, HttpStatus.NO_CONTENT);
                            }
                        }
                        else{
                            String message = "No assets were found in the request.";
                            jobResponse.setMessage(message);

                            _logger.debug("<< packageAssets()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.NOT_FOUND);
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
                String message = "Selected assets are null!";
                jobResponse.setMessage(message);

                _logger.debug("<< packageAssets()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while packaging a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< packageAssets()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/jobs/import", method = RequestMethod.POST)
    public ResponseEntity<JobResponse> importAsset(Authentication authentication, @RequestBody UploadFileLst uploadFileLst) {
        _logger.debug("importAsset() >>");
        try{
            JobResponse jobResponse = new JobResponse();

            if(uploadFileLst != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        _logger.debug("Putting values into the parameter map...");
                        List<UploadFile> uploadedFiles = uploadFileLst.getUploadFiles();
                        if(uploadedFiles != null && !uploadedFiles.isEmpty()){
                            JobParametersBuilder builder = new JobParametersBuilder();

                            for(int i = 0; i < uploadedFiles.size(); i++){
                                UploadFile uploadedFile = uploadedFiles.get(i);
                                builder.addString(Constants.JOB_PARAM_UPLOADED_FILE+ "_" + i, uploadedFile.getPath());
                            }
                            builder.addString(Constants.JOB_PARAM_USERNAME, uploadedFiles.get(0).getUsername());
                            builder.addString(Constants.JOB_PARAM_SYSTEM_MILLIS, String.valueOf(System.currentTimeMillis()));

                            _logger.debug("Launching job [" + importJob.getName() + "]...");
                            JobExecution jobExecution = jobLauncher.run(importJob, builder.toJobParameters());

                            jobResponse.setJobId(jobExecution.getJobId());
                            jobResponse.setMessage("Import job started.");

                            _logger.debug("<< importAsset()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
                        }
                        else{
                            String errorMessage = "Uploaded files list is null or empty!";
                            _logger.error(errorMessage);

                            jobResponse.setMessage(errorMessage);

                            _logger.debug("<< importAsset()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
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
                String errorMessage = "Uploaded files request is null!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                _logger.debug("<< importAsset()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while importing a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< importAsset()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/jobs/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobs(Authentication authentication, @RequestBody JobSearchRequest jobSearchRequest) {
        _logger.debug("retrieveJobs() >>");

        try{
            RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();

            if(jobSearchRequest != null){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        String sortColumn = jobSearchRequest.getSortColumn();
                        String sortType = jobSearchRequest.getSortType();
                        int offset = jobSearchRequest.getOffset();
                        int limit = jobSearchRequest.getLimit();
                        List<SearchRequestFacet> jobSearchRequestFacetList = jobSearchRequest.getSearchRequestFacetList();

                        List<ToyboxJob> allJobs = (List<ToyboxJob>) jobsRepository.getAll();

                        if(!allJobs.isEmpty()){
                            List<ToyboxJob> jobs;

                            if(jobSearchRequestFacetList != null && !jobSearchRequestFacetList.isEmpty()){
                                jobs = allJobs.stream().filter(j -> FacetUtils.getInstance().hasFacetValue(j, jobSearchRequestFacetList)).collect(Collectors.toList());
                            }
                            else{
                                jobs = allJobs;
                            }

                            List<Facet> facets = FacetUtils.getInstance().getFacets(jobs);

                            retrieveToyboxJobsResult.setFacets(facets);

                            if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("JOB_NAME")){
                                SortUtils.getInstance().sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getJobName, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("JOB_TYPE")){
                                SortUtils.getInstance().sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getJobType, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("START_TIME")){
                                SortUtils.getInstance().sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("END_TIME")){
                                SortUtils.getInstance().sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("STATUS")){
                                SortUtils.getInstance().sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getStatus, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            else{
                                SortUtils.getInstance().sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
                            }

                            String username = authentication.getName();
                            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

                            List<ToyboxJob> jobsByCurrentUser;
                            if(authorities.contains("ROLE_ADMIN")){
                                _logger.debug("Retrieving all jobs [Admin User]...");
                                jobsByCurrentUser = jobs;
                            }
                            else{
                                _logger.debug("Retrieving jobs of the user '" + username + "'...");
                                jobsByCurrentUser = jobs.stream().filter(j -> j.getUsername() != null && j.getUsername().equalsIgnoreCase(username)).collect(Collectors.toList());
                            }

                            int totalRecords = jobsByCurrentUser.size();
                            int startIndex = offset;
                            int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                            List<ToyboxJob> jobsOnPage = jobsByCurrentUser.subList(startIndex, endIndex);


                            retrieveToyboxJobsResult.setTotalRecords(totalRecords);
                            retrieveToyboxJobsResult.setJobs(jobsOnPage);

                            _logger.debug("<< retrieveJobs()");
                            retrieveToyboxJobsResult.setMessage("Jobs retrieved successfully!");
                            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.OK);
                        }
                        else{
                            String message = "There is no jobs to return.";
                            _logger.debug(message);

                            retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
                            retrieveToyboxJobsResult.setMessage(message);

                            _logger.debug("<< retrieveJobs()");
                            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.NO_CONTENT);
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

    @RequestMapping(value = "/jobs/{jobInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<RetrieveToyboxJobResult> retrieveJob(Authentication authentication, @PathVariable String jobInstanceId){
        _logger.debug("retrieveJob() >>");
        try{
            RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

            if(StringUtils.isNotBlank(jobInstanceId)){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        List<ToyboxJob> jobs = (List<ToyboxJob>) jobsRepository.getJobsByInstanceId(jobInstanceId);
                        if(!jobs.isEmpty()){
                            if(jobs.size() == 1){
                                ToyboxJob toyboxJob = jobs.get(0);

                                List<ToyboxJobStep> jobSteps = (List<ToyboxJobStep>) jobStepsRepository.getJobStepsByJobExecutionId(toyboxJob.getJobExecutionId());
                                toyboxJob.setSteps(jobSteps);

                                retrieveToyboxJobResult.setToyboxJob(toyboxJob);

                                retrieveToyboxJobResult.setMessage("Job retrieved successfully!");

                                _logger.debug("<< retrieveJob()");
                                return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.OK);
                            }
                            else{
                                String errorMessage = "There are more than one job with the same ID!";
                                _logger.error(errorMessage);

                                retrieveToyboxJobResult.setMessage(errorMessage);

                                _logger.debug("<< retrieveJob()");
                                return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else{
                            String message = "There is no job to return.";
                            _logger.debug(message);

                            retrieveToyboxJobResult.setMessage(message);

                            _logger.debug("<< retrieveJob()");
                            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.NO_CONTENT);
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
                String errorMessage = "Job Instance ID is blank!";
                _logger.error(errorMessage);

                retrieveToyboxJobResult.setMessage(errorMessage);

                _logger.debug("<< retrieveJob()");
                return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

            retrieveToyboxJobResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJob()");
            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/jobs/download/{jobInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadJobResult(Authentication authentication, @PathVariable String jobInstanceId){
        _logger.debug("downloadJobResult() >>");
        try{
            if(StringUtils.isNotBlank(jobInstanceId)){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        String downloadFilePath =  exportStagingPath + File.separator + jobInstanceId + File.separator + "Download.zip";
                        File file = new File(downloadFilePath);

                        if(file.exists()){
                            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                            _logger.debug("<< downloadJobResult()");
                            return new ResponseEntity<>(resource, HttpStatus.OK);
                        }
                        else{
                            throw new IOException("File '" + downloadFilePath + "' does not exist!");
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
                String errorMessage = "Job instance ID is blank!";
                _logger.error(errorMessage);

                _logger.debug("<< downloadJobResult()");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while download the export job result. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< downloadJobResult()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/jobs/stop/{jobInstanceId}", method = RequestMethod.POST)
    public ResponseEntity<JobResponse> stopJob(Authentication authentication, @PathVariable String jobInstanceId) {
        _logger.debug("stopJob() >>");
        try{
            JobResponse jobResponse = new JobResponse();

            if(StringUtils.isNotBlank(jobInstanceId)){
                List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
                if(!usersByUsername.isEmpty()){
                    if(usersByUsername.size() == 1){
                        boolean stop = jobOperator.stop(Long.parseLong(jobInstanceId));
                        if(stop){
                            jobResponse.setJobId(Long.parseLong(jobInstanceId));
                            jobResponse.setMessage("Job stopped successfully.");

                            _logger.debug("<< stopJob()");
                            return new ResponseEntity<>(jobResponse, HttpStatus.OK);
                        }
                        else{
                            throw new JobExecutionNotStoppedException("Job with ID '" + jobInstanceId + "' failed to stop.");
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
                String errorMessage = "Job instance ID is blank!";
                _logger.error(errorMessage);

                jobResponse.setMessage(errorMessage);

                _logger.debug("<< stopJob()");
                return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while stopping the job with ID " + jobInstanceId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            JobResponse jobResponse = new JobResponse();
            jobResponse.setMessage(errorMessage);

            _logger.debug("<< stopJob()");
            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
