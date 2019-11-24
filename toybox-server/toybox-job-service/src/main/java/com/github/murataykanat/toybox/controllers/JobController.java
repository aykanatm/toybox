package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.models.job.ToyboxJob;
import com.github.murataykanat.toybox.models.job.ToyboxJobStep;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.job.*;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
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
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class JobController {
    private static final Log _logger = LogFactory.getLog(JobController.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private SortUtils sortUtils;
    @Autowired
    private FacetUtils facetUtils;

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
    private JobsRepository jobsRepository;
    @Autowired
    JobStepsRepository jobStepsRepository;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/jobs/package", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobResponse> packageAssets(Authentication authentication, @RequestBody SelectionContext selectionContext){
        JobResponse jobResponse = new JobResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(selectionContext != null){
                    List<String> selectedAssetIds = new ArrayList<>();
                    List<String> selectedContainerIds = new ArrayList<>();

                    if(selectionContext.getSelectedAssets() != null){
                        selectedAssetIds = selectionContext.getSelectedAssets().stream().map(Asset::getId).collect(Collectors.toList());
                    }

                    if(selectionContext.getSelectedContainers() != null){
                        selectedContainerIds = selectionContext.getSelectedContainers().stream().map(Container::getId).collect(Collectors.toList());
                    }

                    if(!selectedAssetIds.isEmpty() || !selectedContainerIds.isEmpty()){
                        JobParametersBuilder builder = new JobParametersBuilder();

                        for(int i = 0; i < selectedAssetIds.size(); i++){
                            String assetId = selectedAssetIds.get(i);
                            builder.addString(ToyboxConstants.JOB_PARAM_PACKAGING_FILE + "_" + i, assetId);
                        }

                        for(int i = 0; i < selectedContainerIds.size(); i++){
                            String containerId = selectedContainerIds.get(i);
                            builder.addString(ToyboxConstants.JOB_PARAM_PACKAGING_FOLDER + "_" + i, containerId);
                        }

                        builder.addString(ToyboxConstants.JOB_PARAM_USERNAME, authentication.getName());
                        builder.addString(ToyboxConstants.JOB_PARAM_SYSTEM_MILLIS, String.valueOf(System.currentTimeMillis()));

                        _logger.debug("Launching job [" + packagingJob.getName() + "]...");
                        JobExecution jobExecution = jobLauncher.run(packagingJob, builder.toJobParameters());
                        jobExecution.getExecutionContext().put("jobId", jobExecution.getJobId());

                        jobResponse.setJobId(jobExecution.getJobId());
                        jobResponse.setMessage("Packaging job started.");

                        return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
                    }
                    else{
                        String message = "No assets or containers were found in the request.";
                        jobResponse.setMessage(message);

                        return new ResponseEntity<>(jobResponse, HttpStatus.NOT_FOUND);
                    }
                }
                else{
                    String message = "Selection context is null!";
                    jobResponse.setMessage(message);

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
            String errorMessage = "An error occurred while packaging a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/jobs/import", method = RequestMethod.POST)
    public ResponseEntity<JobResponse> importAsset(Authentication authentication, @RequestBody UploadFileLst uploadFileLst) {
        JobResponse jobResponse = new JobResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(uploadFileLst != null){
                    _logger.debug("Putting values into the parameter map...");
                    List<UploadFile> uploadedFiles = uploadFileLst.getUploadFiles();
                    if(uploadedFiles != null){
                        if(!uploadedFiles.isEmpty()){
                            JobParametersBuilder builder = new JobParametersBuilder();

                            for(int i = 0; i < uploadedFiles.size(); i++){
                                UploadFile uploadedFile = uploadedFiles.get(i);
                                builder.addString(ToyboxConstants.JOB_PARAM_UPLOADED_FILE+ "_" + i, uploadedFile.getPath());
                            }
                            builder.addString(ToyboxConstants.JOB_PARAM_USERNAME, uploadedFiles.get(0).getUsername());
                            builder.addString(ToyboxConstants.JOB_PARAM_CONTAINER_ID, uploadFileLst.getContainerId());
                            builder.addString(ToyboxConstants.JOB_PARAM_SYSTEM_MILLIS, String.valueOf(System.currentTimeMillis()));

                            _logger.debug("Launching job [" + importJob.getName() + "]...");
                            JobExecution jobExecution = jobLauncher.run(importJob, builder.toJobParameters());

                            jobResponse.setJobId(jobExecution.getJobId());
                            jobResponse.setMessage("Import job started.");

                            return new ResponseEntity<>(jobResponse, HttpStatus.CREATED);
                        }
                        else{
                            String errorMessage = "Uploaded files list is empty, nothing to import.";
                            _logger.warn(errorMessage);

                            jobResponse.setMessage(errorMessage);

                            return new ResponseEntity<>(jobResponse, HttpStatus.NO_CONTENT);
                        }
                    }
                    else{
                        String errorMessage = "Uploaded files list is null!";
                        _logger.error(errorMessage);

                        jobResponse.setMessage(errorMessage);

                        return new ResponseEntity<>(jobResponse, HttpStatus.BAD_REQUEST);
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
            String errorMessage = "An error occurred while importing a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/jobs/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobs(Authentication authentication, @RequestBody JobSearchRequest jobSearchRequest) {
        RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(jobSearchRequest != null){
                    String sortColumn = jobSearchRequest.getSortColumn();
                    String sortType = jobSearchRequest.getSortType();
                    int offset = jobSearchRequest.getOffset();
                    int limit = jobSearchRequest.getLimit();
                    List<SearchRequestFacet> jobSearchRequestFacetList = jobSearchRequest.getSearchRequestFacetList();

                    List<ToyboxJob> allJobs = jobsRepository.getAll();

                    if(!allJobs.isEmpty()){
                        List<ToyboxJob> jobs;

                        if(jobSearchRequestFacetList != null && !jobSearchRequestFacetList.isEmpty()){
                            jobs = allJobs.stream().filter(j -> facetUtils.hasFacetValue(j, jobSearchRequestFacetList)).collect(Collectors.toList());
                        }
                        else{
                            jobs = allJobs;
                        }

                        List<Facet> facets = facetUtils.getFacets(jobs);

                        retrieveToyboxJobsResult.setFacets(facets);

                        if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("JOB_NAME")){
                            sortUtils.sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getJobName, Comparator.nullsLast(Comparator.naturalOrder())));
                        }
                        else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("JOB_TYPE")){
                            sortUtils.sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getJobType, Comparator.nullsLast(Comparator.naturalOrder())));
                        }
                        else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("START_TIME")){
                            sortUtils.sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
                        }
                        else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("END_TIME")){
                            sortUtils.sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
                        }
                        else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("STATUS")){
                            sortUtils.sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getStatus, Comparator.nullsLast(Comparator.naturalOrder())));
                        }
                        else{
                            sortUtils.sortItems(sortType, jobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
                        }

                        List<ToyboxJob> jobsByCurrentUser;

                        if(authenticationUtils.isAdminUser(authentication)){
                            _logger.debug("Retrieving all jobs [Admin User]...");
                            jobsByCurrentUser = jobs;
                        }
                        else{
                            _logger.debug("Retrieving jobs of the user '" + authentication.getName() + "'...");
                            jobsByCurrentUser = jobs.stream().filter(j -> j.getUsername() != null && j.getUsername().equalsIgnoreCase(authentication.getName())).collect(Collectors.toList());
                        }

                        int totalRecords = jobsByCurrentUser.size();
                        if(offset > totalRecords){
                            offset = 0;
                        }
                        int endIndex = Math.min((offset + limit), totalRecords);

                        List<ToyboxJob> jobsOnPage = jobsByCurrentUser.subList(offset, endIndex);


                        retrieveToyboxJobsResult.setTotalRecords(totalRecords);
                        retrieveToyboxJobsResult.setJobs(jobsOnPage);

                        retrieveToyboxJobsResult.setMessage("Jobs retrieved successfully!");
                        return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.OK);
                    }
                    else{
                        String message = "There is no jobs to return.";
                        _logger.debug(message);

                        retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
                        retrieveToyboxJobsResult.setMessage(message);

                        return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.NO_CONTENT);
                    }
                }
                else{
                    String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                    _logger.error(errorMessage);

                    retrieveToyboxJobsResult.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.UNAUTHORIZED);
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
    @RequestMapping(value = "/jobs/{jobInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<RetrieveToyboxJobResult> retrieveJob(Authentication authentication, @PathVariable String jobInstanceId){
        RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(jobInstanceId)){
                    List<ToyboxJob> jobs = jobsRepository.getJobsByInstanceId(jobInstanceId);
                    if(!jobs.isEmpty()){
                        if(jobs.size() == 1){
                            ToyboxJob toyboxJob = jobs.get(0);

                            List<ToyboxJobStep> jobSteps = jobStepsRepository.getJobStepsByJobExecutionId(toyboxJob.getJobExecutionId());
                            toyboxJob.setSteps(jobSteps);

                            retrieveToyboxJobResult.setToyboxJob(toyboxJob);

                            retrieveToyboxJobResult.setMessage("Job retrieved successfully!");

                            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.OK);
                        }
                        else{
                            String errorMessage = "There are more than one job with the same ID!";
                            _logger.error(errorMessage);

                            retrieveToyboxJobResult.setMessage(errorMessage);

                            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                    else{
                        String message = "There is no job to return.";
                        _logger.debug(message);

                        retrieveToyboxJobResult.setMessage(message);

                        return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.NO_CONTENT);
                    }
                }
                else{
                    String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                    _logger.error(errorMessage);

                    retrieveToyboxJobResult.setMessage(errorMessage);

                    return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.UNAUTHORIZED);
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
            String errorMessage = "An error occurred while retrieving the job. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            retrieveToyboxJobResult.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/jobs/download/{jobInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadJobResult(Authentication authentication, @PathVariable String jobInstanceId){
        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(jobInstanceId)){
                    String downloadFilePath =  exportStagingPath + File.separator + jobInstanceId + File.separator + "Download.zip";
                    File file = new File(downloadFilePath);

                    if(file.exists()){
                        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                        return new ResponseEntity<>(resource, HttpStatus.OK);
                    }
                    else{
                        throw new IOException("File '" + downloadFilePath + "' does not exist!");
                    }
                }
                else{
                    String errorMessage = "Job instance ID is blank!";
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
            String errorMessage = "An error occurred while download the export job result. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/jobs/stop/{jobInstanceId}", method = RequestMethod.POST)
    public ResponseEntity<JobResponse> stopJob(Authentication authentication, @PathVariable String jobInstanceId) {
        JobResponse jobResponse = new JobResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                if(StringUtils.isNotBlank(jobInstanceId)){
                    boolean stop = jobOperator.stop(Long.parseLong(jobInstanceId));
                    if(stop){
                        jobResponse.setJobId(Long.parseLong(jobInstanceId));
                        jobResponse.setMessage("Job stopped successfully.");

                        return new ResponseEntity<>(jobResponse, HttpStatus.OK);
                    }
                    else{
                        throw new JobExecutionNotStoppedException("Job with ID '" + jobInstanceId + "' failed to stop.");
                    }
                }
                else{
                    String errorMessage = "Job instance ID is blank!";
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
            String errorMessage = "An error occurred while stopping the job with ID " + jobInstanceId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            jobResponse.setMessage(errorMessage);

            return new ResponseEntity<>(jobResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}