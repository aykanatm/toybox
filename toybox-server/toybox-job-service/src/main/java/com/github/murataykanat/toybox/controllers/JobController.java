package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.batch.utils.Constants;
import com.github.murataykanat.toybox.models.*;
import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import com.github.murataykanat.toybox.models.dbo.mappers.ToyboxJobRowMapper;
import com.github.murataykanat.toybox.models.dbo.mappers.ToyboxJobStepRowMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@RestController
public class JobController {
    private static final Log _logger = LogFactory.getLog(JobController.class);
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job importJob;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RequestMapping(value = "/jobs/import", method = RequestMethod.POST)
    public ResponseEntity<ImportAssetResponse> importAsset(@RequestBody String uploadedFiles) {
        _logger.debug("importAsset() >>");
        try{
            if(StringUtils.isNotBlank(uploadedFiles)){
                ImportAssetResponse importAssetResponse = new ImportAssetResponse();
                _logger.debug("Uploaded files: ");
                _logger.debug(uploadedFiles);

                _logger.debug("Putting values into the parameter map...");
                JobParametersBuilder builder = new JobParametersBuilder();
                builder.addString(Constants.JOB_PARAM_UPLOADED_FILES, uploadedFiles);
                builder.addString(Constants.JOB_PARAM_SYSTEM_MILLIS, String.valueOf(System.currentTimeMillis()));

                _logger.debug("Launching job [" + importJob.getName() + "]...");
                JobExecution jobExecution = jobLauncher.run(importJob, builder.toJobParameters());

                importAssetResponse.setJobId(jobExecution.getJobId());
                importAssetResponse.setMessage("Import job started.");

                _logger.debug("<< importAsset()");
                return new ResponseEntity<>(importAssetResponse, HttpStatus.CREATED);
            }
            else{
                String errorMessage = "Uploaded files request is blank!";
                _logger.error(errorMessage);
                ImportAssetResponse importAssetResponse = new ImportAssetResponse();
                importAssetResponse.setMessage(errorMessage);

                _logger.debug("<< importAsset()");
                return new ResponseEntity<>(importAssetResponse, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while importing a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
            ImportAssetResponse importAssetResponse = new ImportAssetResponse();
            importAssetResponse.setMessage(errorMessage);

            _logger.debug("<< importAsset()");
            return new ResponseEntity<>(importAssetResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/jobs/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveToyboxJobsResult> retrieveJobs(@RequestBody JobSearchRequest jobSearchRequest) {
        _logger.debug("retrieveJobs() >>");

        try{
            if(jobSearchRequest != null){
                RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();

                String sortColumn = jobSearchRequest.getSortColumn();
                String sortType = jobSearchRequest.getSortType();
                String username = jobSearchRequest.getUsername();
                int offset = jobSearchRequest.getOffset();
                int limit = jobSearchRequest.getLimit();
                List<JobSearchRequestFacet> jobSearchRequestFacetList = jobSearchRequest.getJobSearchRequestFacetList();

                List<ToyboxJob> allJobs = jdbcTemplate.query("SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID, JOB_NAME, JOB_TYPE, START_TIME, END_TIME, STATUS, PARAMETERS  FROM TOYBOX_JOBS_VW", new ToyboxJobRowMapper());

                if(allJobs.size() > 0){
                    List<ToyboxJob> jobs;

                    if(jobSearchRequestFacetList != null && jobSearchRequestFacetList.size() > 0){
                        jobs = allJobs.stream().filter(j -> j.hasFacetValue(jobSearchRequestFacetList)).collect(Collectors.toList());
                    }
                    else{
                        jobs = allJobs;
                    }

                    List<String> facets = Arrays.asList(ToyboxJob.class.getDeclaredFields())
                            .stream()
                            .filter(f -> nonNull(f.getAnnotation(FacetColumnName.class)))
                            .map(f -> f.getAnnotation(FacetColumnName.class).value())
                            .collect(Collectors.toList());

                    List<ToyboxJobFacet> toyboxJobFacets = new ArrayList<>();

                    // TODO: Convert this to stream implementation
                    for(String facetName: facets){
                        ToyboxJobFacet toyboxJobFacet = new ToyboxJobFacet();
                        toyboxJobFacet.setName(facetName);

                        List<String> lookups = new ArrayList<>();

                        for(ToyboxJob toyboxJob: jobs ){
                            for(Method method: toyboxJob.getClass().getDeclaredMethods()){
                                if(method.getAnnotation(FacetColumnName.class) != null)
                                {
                                    String facetColumnName = method.getAnnotation(FacetColumnName.class).value();
                                    if(facetColumnName != null && facetColumnName.equalsIgnoreCase(facetName)){
                                        if(method.getAnnotation(FacetDefaultLookup.class) != null)
                                        {
                                            String[] defaultLookups = method.getAnnotation(FacetDefaultLookup.class).values();
                                            for(String defaultLookup: defaultLookups){
                                                lookups.add(defaultLookup);
                                            }
                                            break;
                                        }
                                        else
                                        {
                                            String lookup = (String) method.invoke(toyboxJob);
                                            lookups.add(lookup);
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        Set<String> uniqueLookupValues = new HashSet<>(lookups);
                        toyboxJobFacet.setLookups(new ArrayList<>(uniqueLookupValues));
                        toyboxJobFacets.add(toyboxJobFacet);
                    }

                    retrieveToyboxJobsResult.setFacets(toyboxJobFacets);

                    if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("JOB_NAME")){
                        sortJobs(sortType, jobs, Comparator.comparing(ToyboxJob::getJobName, Comparator.nullsLast(Comparator.naturalOrder())));
                    }
                    else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("JOB_TYPE")){
                        sortJobs(sortType, jobs, Comparator.comparing(ToyboxJob::getJobType, Comparator.nullsLast(Comparator.naturalOrder())));
                    }
                    else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("START_TIME")){
                        sortJobs(sortType, jobs, Comparator.comparing(ToyboxJob::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
                    }
                    else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("END_TIME")){
                        sortJobs(sortType, jobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
                    }
                    else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("STATUS")){
                        sortJobs(sortType, jobs, Comparator.comparing(ToyboxJob::getStatus, Comparator.nullsLast(Comparator.naturalOrder())));
                    }
                    else{
                        sortJobs(sortType, jobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
                    }

                    // TODO: If an admin users gets the jobs, display all jobs regardless of the username
                    List<ToyboxJob> jobsByCurrentUser = jobs.stream().filter(j -> j.getUsername().equalsIgnoreCase(username)).collect(Collectors.toList());

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
                String errorMessage = "Job search request is null!";
                _logger.error(errorMessage);

                RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
                retrieveToyboxJobsResult.setMessage(errorMessage);

                _logger.debug("<< retrieveJobs()");
                return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the jobs. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();
            retrieveToyboxJobsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobs()");
            return new ResponseEntity<>(retrieveToyboxJobsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sortJobs(String sortType, List<ToyboxJob> allJobs, Comparator<ToyboxJob> comparing) {
        if(sortType.equalsIgnoreCase("des")){
            allJobs.sort(comparing.reversed());
        }
        else if(sortType.equalsIgnoreCase("asc")){
            allJobs.sort(comparing);
        }
        else{
            allJobs.sort(comparing.reversed());
        }
    }

    @RequestMapping(value = "/jobs/{jobInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<RetrieveToyboxJobResult> retrieveJob(@PathVariable String jobInstanceId){
        _logger.debug("retrieveJob() >>");
        try{
            if(StringUtils.isNotBlank(jobInstanceId)){
                List<ToyboxJob> jobs = jdbcTemplate.query("SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID, JOB_NAME, JOB_TYPE, START_TIME, END_TIME, STATUS, PARAMETERS  FROM TOYBOX_JOBS_VW WHERE JOB_INSTANCE_ID=?",
                        new Object[]{jobInstanceId},new ToyboxJobRowMapper());
                if(jobs.size() > 0){
                    if(jobs.size() == 1){
                        RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

                        ToyboxJob toyboxJob = jobs.get(0);
                        retrieveToyboxJobResult.setToyboxJob(toyboxJob);

                        List<ToyboxJobStep> jobSteps = jdbcTemplate.query("SELECT JOB_EXECUTION_ID, STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS  FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID=?",
                                new Object[]{toyboxJob.getJobExecutionId()},new ToyboxJobStepRowMapper());
                        retrieveToyboxJobResult.setToyboxJobSteps(jobSteps);
                        retrieveToyboxJobResult.setMessage("Job retrieved successfully!");

                        _logger.debug("<< retrieveJob()");
                        return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.OK);
                    }
                    else{
                        String errorMessage = "There are more than one job with the same ID!";
                        _logger.error(errorMessage);

                        RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();

                        retrieveToyboxJobResult.setMessage(errorMessage);

                        _logger.debug("<< retrieveJob()");
                        return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                else{
                    String message = "There is no job to return.";
                    _logger.debug(message);

                    RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
                    retrieveToyboxJobResult.setMessage(message);

                    _logger.debug("<< retrieveJob()");
                    return new ResponseEntity<>(retrieveToyboxJobResult, HttpStatus.NO_CONTENT);
                }
            }
            else{
                String errorMessage = "Job Instance ID is blank!";
                _logger.error(errorMessage);

                RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
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

    @RequestMapping(value = "/jobs/{jobExecutionId}/steps", method = RequestMethod.GET)
    public ResponseEntity<RetrieveJobStepsResult> retrieveJobSteps(@PathVariable String jobExecutionId){
        _logger.debug("retrieveJobSteps() >>");
        try{
            if(StringUtils.isNotBlank(jobExecutionId)){
                RetrieveJobStepsResult retrieveJobStepsResult = new RetrieveJobStepsResult();

                List<ToyboxJobStep> jobSteps = jdbcTemplate.query("SELECT JOB_EXECUTION_ID, STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS  FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID=?",
                        new Object[]{jobExecutionId},new ToyboxJobStepRowMapper());

                if(jobSteps.size() > 0){
                    retrieveJobStepsResult.setToyboxJobSteps(jobSteps);
                    retrieveJobStepsResult.setMessage("Job steps retrieved successfully!");

                    _logger.debug("<< retrieveJobSteps()");
                    return new ResponseEntity<>(retrieveJobStepsResult, HttpStatus.OK);
                }
                else{
                    String message = "There is no job to return.";
                    _logger.debug(message);

                    retrieveJobStepsResult = new RetrieveJobStepsResult();
                    retrieveJobStepsResult.setMessage(message);

                    _logger.debug("<< retrieveJobSteps()");
                    return new ResponseEntity<>(retrieveJobStepsResult, HttpStatus.NO_CONTENT);
                }
            }
            else{
                String errorMessage = "Job Execution ID is blank!";
                _logger.error(errorMessage);

                RetrieveJobStepsResult retrieveJobStepsResult = new RetrieveJobStepsResult();
                retrieveJobStepsResult.setMessage(errorMessage);

                _logger.debug("<< retrieveJobSteps()");
                return new ResponseEntity<>(retrieveJobStepsResult, HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the job steps. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveJobStepsResult retrieveJobStepsResult = new RetrieveJobStepsResult();
            retrieveJobStepsResult.setMessage(errorMessage);

            _logger.debug("<< retrieveJobSteps()");
            return new ResponseEntity<>(retrieveJobStepsResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
