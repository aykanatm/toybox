package com.github.murataykanat.toybox.toyboxjobservice.controllers;

import com.github.murataykanat.toybox.toyboxjobservice.batch.config.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class JobController {
    private static final Log _logger = LogFactory.getLog(JobController.class);
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private Job job;

    @RequestMapping(value = "/job/import", method = RequestMethod.POST)
    public String importAsset(@RequestBody String uploadedFiles) throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
        _logger.debug("importAsset() >>");
        try{
            _logger.debug("Uploaded files: ");
            _logger.debug(uploadedFiles);

            _logger.debug("Putting values into the parameter map...");
            JobParametersBuilder builder = new JobParametersBuilder();
            builder.addString(Constants.JOB_PARAM_UPLOADED_FILES, uploadedFiles);

            _logger.debug("Launching job [" + job.getName() + "]...");
            JobExecution jobExecution = jobLauncher.run(job, builder.toJobParameters());

            _logger.debug("<< importAsset()");
            return job.getName();
        }
        catch (Exception e){
            String errorMessage = "An error occured while importing a batch. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
            throw e;
        }
    }
}
