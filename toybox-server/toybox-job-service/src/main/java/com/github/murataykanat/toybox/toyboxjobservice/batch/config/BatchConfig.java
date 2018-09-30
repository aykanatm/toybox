package com.github.murataykanat.toybox.toyboxjobservice.batch.config;

import com.github.murataykanat.toybox.toyboxjobservice.models.UploadFile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
    private static final Log _logger = LogFactory.getLog(BatchConfig.class);

    @Bean
    public Job job(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory){
        Step step = stepBuilderFactory.get(Constants.STEP_NAME)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >>");
                        _logger.debug("<< execute()");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        return jobBuilderFactory.get(Constants.JOB_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(validator())
                .start(step)
                .build();
    }

    public JobParametersValidator validator(){
        return new JobParametersValidator(){
            @Override
            public void validate(JobParameters parameters) throws JobParametersInvalidException {
                _logger.debug("validate() >>");

                String filePathsJsonStr = parameters.getString(Constants.JOB_PARAM_UPLOADED_FILES);
                if(StringUtils.isBlank(filePathsJsonStr)){
                    throw new JobParametersInvalidException("'" + Constants.JOB_PARAM_UPLOADED_FILES  + "' parameter is required for job '" + Constants.JOB_NAME + "'.");
                }

                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<UploadFile>>(){}.getType();
                ArrayList<UploadFile> uploadedFiles = gson.fromJson(filePathsJsonStr, listType);

                for(UploadFile uploadFile: uploadedFiles){
                    File file = new File(uploadFile.getPath());
                    if(!file.exists()){
                        throw new JobParametersInvalidException("File '" + uploadFile.getPath() + "' did not exist or was not readable.");
                    }
                }

                _logger.debug("<< validate()");
            }
        };
    }
}
