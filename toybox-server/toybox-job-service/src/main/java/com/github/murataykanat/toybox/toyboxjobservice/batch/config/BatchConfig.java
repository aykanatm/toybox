package com.github.murataykanat.toybox.toyboxjobservice.batch.config;

import com.github.murataykanat.toybox.toyboxjobservice.models.UploadFile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.exec.*;
import org.apache.commons.lang.RandomStringUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RefreshScope
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    private static final Log _logger = LogFactory.getLog(BatchConfig.class);

    @Value("${thumbnailFormat}")
    private String thumbnailFormat;
    @Value("${imagemagickExecutable}")
    private String imagemagickExecutable;
    @Value("${imagemagickThumbnailSettings}")
    private String imagemagickThumbnailSettings;
    @Value("${repositoryPath}")
    private String repositoryPath;

    @Bean
    public Job importJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory){
        Step step = stepBuilderFactory.get(Constants.STEP_IMPORT_START)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_START + "]");
                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        for(Map.Entry<String, Object> entry: jobParameters.entrySet()){
                            if(entry.getKey().equalsIgnoreCase(Constants.JOB_PARAM_UPLOADED_FILES)){
                                String filePathsJsonStr = (String) entry.getValue();

                                Gson gson = new Gson();
                                Type listType = new TypeToken<ArrayList<UploadFile>>(){}.getType();
                                ArrayList<UploadFile> uploadedFiles = gson.fromJson(filePathsJsonStr, listType);

                                for(UploadFile uploadFile: uploadedFiles){
                                    String assetId = RandomStringUtils.randomAlphanumeric(Constants.ASSET_ID_LENGTH);
                                    String assetFolderPath = repositoryPath + File.separator + assetId;
                                    String assetThumbnailPath = repositoryPath + File.separator + assetId + File.separator + "thumbnail";
                                    createAssetFolders(assetFolderPath, assetThumbnailPath);

                                    // TODO:
                                    // Add exception handling
                                    // Implement saving asset to database

                                    // Copy file to repository
                                    File source = new File(uploadFile.getPath());
                                    File destination = new File(assetFolderPath + File.separator + source.getName());
                                    FileSystemUtils.copyRecursively(source, destination);

                                    // Generate thumbnail
                                    CommandLine cmdLine = new CommandLine(imagemagickExecutable);
                                    cmdLine.addArgument("${inputFile}");
                                    String[] arguments = imagemagickThumbnailSettings.split("\\s+");
                                    for(String argument: arguments){
                                        cmdLine.addArgument(argument);
                                    }
                                    cmdLine.addArgument("${outputFile}");
                                    HashMap map = new HashMap();
                                    map.put("inputFile", new File(uploadFile.getPath()));
                                    map.put("outputFile", new File(assetThumbnailPath
                                            + File.separator + assetId + "." + thumbnailFormat));
                                    cmdLine.setSubstitutionMap(map);

                                    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

                                    ExecuteWatchdog watchdog = new ExecuteWatchdog(Constants.IMAGEMAGICK_TIMEOUT);
                                    Executor executor = new DefaultExecutor();
                                    executor.setExitValue(1);
                                    executor.setWatchdog(watchdog);
                                    _logger.debug("Executing command: " + cmdLine.toString());
                                    executor.execute(cmdLine, resultHandler);

                                    resultHandler.waitFor();
                                    _logger.debug("Exception: " + resultHandler.getException().getLocalizedMessage());
                                    _logger.debug("Exit value: " + resultHandler.getExitValue());
                                }
                            }
                        }
                        _logger.debug("<< execute()");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        return jobBuilderFactory.get(Constants.JOB_IMPORT_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(importValidator())
                .start(step)
                .build();
    }

    public JobParametersValidator importValidator(){
        return new JobParametersValidator(){
            @Override
            public void validate(JobParameters parameters) throws JobParametersInvalidException {
                _logger.debug("validate() >>");

                String filePathsJsonStr = parameters.getString(Constants.JOB_PARAM_UPLOADED_FILES);
                if(StringUtils.isBlank(filePathsJsonStr)){
                    throw new JobParametersInvalidException("'" + Constants.JOB_PARAM_UPLOADED_FILES
                            + "' parameter is required for job '" + Constants.JOB_IMPORT_NAME + "'.");
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

    public void createAssetFolders(String assetFolderPath, String assetThumbnailPath){
        String[] paths = new String[]{assetFolderPath, assetThumbnailPath};
        for(String path: paths){
            File directory = new File(path);
            if(!directory.exists()){
                directory.mkdir();
            }
        }
    }
}
