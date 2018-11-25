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
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
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
    public Long importAsset(@RequestBody String uploadedFiles) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        _logger.debug("importAsset() >>");
        try{
            _logger.debug("Uploaded files: ");
            _logger.debug(uploadedFiles);

            _logger.debug("Putting values into the parameter map...");
            JobParametersBuilder builder = new JobParametersBuilder();
            builder.addString(Constants.JOB_PARAM_UPLOADED_FILES, uploadedFiles);
            builder.addString(Constants.JOB_PARAM_SYSTEM_MILLIS, String.valueOf(System.currentTimeMillis()));

            _logger.debug("Launching job [" + importJob.getName() + "]...");
            JobExecution jobExecution = jobLauncher.run(importJob, builder.toJobParameters());

            _logger.debug("<< importAsset()");
            return jobExecution.getJobId();
        }
        catch (Exception e){
            String errorMessage = "An error occured while importing a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
            throw e;
        }
    }

    @RequestMapping(value = "/jobs/search", method = RequestMethod.POST)
    public RetrieveToyboxJobsResult retrieveJobs(@RequestBody JobSearchRequest jobSearchRequest) throws InvocationTargetException, IllegalAccessException {
        _logger.debug("retrieveJobs() >>");
        RetrieveToyboxJobsResult retrieveToyboxJobsResult = new RetrieveToyboxJobsResult();

        String sortColumn = jobSearchRequest.getSortColumn();
        String sortType = jobSearchRequest.getSortType();
        String username = jobSearchRequest.getUsername();
        int offset = jobSearchRequest.getOffset();
        int limit = jobSearchRequest.getLimit();

        try{
            List<ToyboxJob> allJobs = jdbcTemplate.query("SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID, JOB_NAME, JOB_TYPE, START_TIME, END_TIME, STATUS, PARAMETERS  FROM TOYBOX_JOBS_VW", new ToyboxJobRowMapper());

            List<String> facets = Arrays.asList(ToyboxJob.class.getDeclaredFields())
                    .stream()
                    .filter(f -> nonNull(f.getAnnotation(FacetColumnName.class)))
                    .map(f -> f.getAnnotation(FacetColumnName.class).value())
                    .collect(Collectors.toList());

            List<ToyboxJobFacet> toyboxJobFacets = new ArrayList<>();

            for(String facetName: facets){
                ToyboxJobFacet toyboxJobFacet = new ToyboxJobFacet();
                toyboxJobFacet.setName(facetName);

                List<String> lookups = new ArrayList<>();

                for(ToyboxJob toyboxJob: allJobs ){
                    for(Method method: toyboxJob.getClass().getDeclaredMethods()){
                        if(method.getAnnotation(FacetColumnName.class) != null)
                        {
                            String facetColumnName = method.getAnnotation(FacetColumnName.class).value();
                            if(facetColumnName != null && facetColumnName.equalsIgnoreCase(facetName)){
                                if(method.getAnnotation(FacetDefaultLookup.class) != null)
                                {
                                    String defaultlookupString = method.getAnnotation(FacetDefaultLookup.class).values();
                                    String[] defaultLookups = defaultlookupString.split(",");
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
                sortJobs(sortType, allJobs, Comparator.comparing(ToyboxJob::getJobName, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("JOB_TYPE")){
                sortJobs(sortType, allJobs, Comparator.comparing(ToyboxJob::getJobType, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("START_TIME")){
                sortJobs(sortType, allJobs, Comparator.comparing(ToyboxJob::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("END_TIME")){
                sortJobs(sortType, allJobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("STATUS")){
                sortJobs(sortType, allJobs, Comparator.comparing(ToyboxJob::getStatus, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            else{
                sortJobs(sortType, allJobs, Comparator.comparing(ToyboxJob::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())));
            }

            // TODO:
            // If an admin users gets the jobs, display all jobs regardless of the username

            List<ToyboxJob> jobsByCurrentUser = allJobs.stream().filter(j -> j.getUsername().equalsIgnoreCase(username)).collect(Collectors.toList());

            int totalRecords = jobsByCurrentUser.size();
            int startIndex = offset;
            int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

            List<ToyboxJob> jobsOnPage = jobsByCurrentUser.subList(startIndex, endIndex);


            retrieveToyboxJobsResult.setTotalRecords(totalRecords);
            retrieveToyboxJobsResult.setJobs(jobsOnPage);
        }
        catch (Exception e){
            String errorMessage = "An error occured while retrieving the jobs. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
            throw e;
        }

        _logger.debug("<< retrieveJobs()");
        return retrieveToyboxJobsResult;
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
    public RetrieveToyboxJobResult retrieveJob(@PathVariable String jobInstanceId){
        _logger.debug("retrieveJob() >>");
        RetrieveToyboxJobResult retrieveToyboxJobResult = new RetrieveToyboxJobResult();
        List<ToyboxJob> jobs = jdbcTemplate.query("SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID, JOB_NAME, JOB_TYPE, START_TIME, END_TIME, STATUS, PARAMETERS  FROM TOYBOX_JOBS_VW WHERE JOB_INSTANCE_ID=?",
                new Object[]{jobInstanceId},new ToyboxJobRowMapper());
        if(jobs.size() == 1){
            ToyboxJob toyboxJob = jobs.get(0);
            retrieveToyboxJobResult.setToyboxJob(toyboxJob);

            List<ToyboxJobStep> jobSteps = jdbcTemplate.query("SELECT JOB_EXECUTION_ID, STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS  FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID=?",
                    new Object[]{toyboxJob.getJobExecutionId()},new ToyboxJobStepRowMapper());
            retrieveToyboxJobResult.setToyboxJobSteps(jobSteps);
        }

        _logger.debug("<< retrieveJob()");
        return retrieveToyboxJobResult;
    }

    @RequestMapping(value = "/jobs/{jobExecutionId}/steps", method = RequestMethod.GET)
    public RetrieveJobStepsResult retrieveJobSteps(@PathVariable String jobExecutionId){
        _logger.debug("retrieveJobSteps() >>");
        RetrieveJobStepsResult retrieveJobStepsResult = new RetrieveJobStepsResult();

        List<ToyboxJobStep> jobSteps = jdbcTemplate.query("SELECT JOB_EXECUTION_ID, STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS  FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID=?",
                new Object[]{jobExecutionId},new ToyboxJobStepRowMapper());

        retrieveJobStepsResult.setToyboxJobSteps(jobSteps);

        _logger.debug("<< retrieveJobSteps()");
        return retrieveJobStepsResult;
    }
}
