package com.github.murataykanat.toybox.batch.jobs;

import com.github.murataykanat.toybox.batch.utils.Constants;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RefreshScope
@Configuration
@EnableBatchProcessing
public class CompressJobConfig {
    private static final Log _logger = LogFactory.getLog(CompressJobConfig.class);

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @Bean
    public Job compressionJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory){
        Step stepCompressAssets = stepBuilderFactory.get(Constants.STEP_COMPRESSION_GENERATE_ARCHIVE)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_COMPRESSION_GENERATE_ARCHIVE + "]");

                        // Create job folder
                        Object obj = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("jobId");
                        long jobId = (long) obj;
                        String jobFolderPath = exportStagingPath + File.separator + jobId;
                        File jobFolder = new File(jobFolderPath);
                        if(!jobFolder.exists()){
                            boolean mkdir = jobFolder.mkdir();
                            List<File> outputFiles = new ArrayList<>();
                            if(mkdir){
                                // Copy assets to export folder
                                Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                                for(Map.Entry<String, Object> jobParameter: jobParameters.entrySet()){
                                    if(jobParameter.getKey().startsWith(Constants.JOB_PARAM_COMPRESSION_FILE)){
                                        String filePath = (String) jobParameter.getValue();

                                        File inputFile = new File(filePath);
                                        File outputFile = new File(jobFolderPath + File.separator + inputFile.getName());
                                        Files.copy(inputFile.toPath(), outputFile.toPath());

                                        outputFiles.add(outputFile);
                                    }
                                }

                                // Generate the archive
                                File archive = new File(jobFolderPath + File.separator + "Download.zip");
                                try(FileOutputStream fos = new FileOutputStream(archive)){
                                    try(ZipOutputStream zos = new ZipOutputStream(fos)){
                                        for(File outputFile: outputFiles){
                                            try(FileInputStream fis = new FileInputStream(outputFile)){
                                                ZipEntry zipEntry = new ZipEntry(outputFile.getName());
                                                zos.putNextEntry(zipEntry);

                                                byte[] bytes = new byte[1024];
                                                int length;
                                                while ((length = fis.read(bytes)) >= 0){
                                                    zos.write(bytes, 0, length);
                                                }
                                            }
                                        }
                                    }
                                }

                                if(archive.exists()){
                                    chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("archiveFilePath", archive.getAbsolutePath());
                                }
                                else{
                                    throw new IOException("Archive file does not exist!");
                                }
                            }
                            else{
                                throw new IOException("Unable to create folder with path '" + jobFolderPath + "'.");
                            }
                        }
                        else{
                            throw new IOException("Job folder '" + jobFolderPath + "' already exists!");
                        }

                        _logger.debug("<< execute() " + Constants.STEP_COMPRESSION_GENERATE_ARCHIVE + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        return jobBuilderFactory.get(Constants.JOB_COMPRESSION_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(compressionValidator())
                .flow(stepCompressAssets)
                .end()
                .build();
    }

    private JobParametersValidator compressionValidator(){
        return new JobParametersValidator(){
            @Override
            public void validate(JobParameters parameters) throws JobParametersInvalidException {
                _logger.debug("validate() >>");

                Map<String, JobParameter> parameterMap = parameters.getParameters();
                for(Map.Entry<String, JobParameter> parameterEntry: parameterMap.entrySet()){
                    if(parameterEntry.getKey().startsWith(Constants.JOB_PARAM_COMPRESSION_FILE)){
                        String filePath = (String) parameterEntry.getValue().getValue();
                        File file = new File(filePath);
                        if(!file.exists()){
                            throw new JobParametersInvalidException("File '" + filePath + "' did not exist or was not readable.");
                        }
                    }
                }

                _logger.debug("<< validate()");
            }
        };
    }
}
