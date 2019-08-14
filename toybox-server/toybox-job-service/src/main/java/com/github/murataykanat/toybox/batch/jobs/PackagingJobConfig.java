package com.github.murataykanat.toybox.batch.jobs;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.batch.utils.Constants;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.dbo.ContainerAsset;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainersRepository;
import com.github.murataykanat.toybox.utilities.AssetUtils;
import com.github.murataykanat.toybox.utilities.ContainerUtils;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RefreshScope
@Configuration
@EnableBatchProcessing
public class PackagingJobConfig {
    private static final Log _logger = LogFactory.getLog(PackagingJobConfig.class);

    @Value("${exportStagingPath}")
    private String exportStagingPath;

    @Autowired
    private ContainersRepository containersRepository;

    @Autowired
    private AssetsRepository assetsRepository;

    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;

    @Bean
    public Job packagingJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory){
        Step stepCompressAssets = stepBuilderFactory.get(Constants.STEP_PACKAGING_GENERATE_ARCHIVE)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_PACKAGING_GENERATE_ARCHIVE + "]");

                        // Create job folder
                        Object obj = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("jobId");
                        long jobId = (long) obj;
                        String jobFolderPath = exportStagingPath + File.separator + jobId;
                        File jobFolder = new File(jobFolderPath);
                        if(!jobFolder.exists()){
                            boolean mkdir = jobFolder.mkdir();
                            if(mkdir){
                                String archiveFilePath = jobFolderPath + File.separator + "Download.zip";

                                File zipFile = new File(archiveFilePath);
                                ZipFile archive = new ZipFile(archiveFilePath);
                                // Copy assets to export folder
                                Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                                for(Map.Entry<String, Object> jobParameter: jobParameters.entrySet()){
                                    if(jobParameter.getKey().startsWith(Constants.JOB_PARAM_PACKAGING_FILE)){
                                        String assetId = (String) jobParameter.getValue();
                                        Asset asset = AssetUtils.getInstance().getAsset(assetsRepository, assetId);
                                        File inputFile = new File(asset.getPath());
                                        File outputFile = new File(jobFolderPath + File.separator + inputFile.getName());
                                        Files.copy(inputFile.toPath(), outputFile.toPath());
                                    }
                                    else if(jobParameter.getKey().startsWith(Constants.JOB_PARAM_PACKAGING_FOLDER)){
                                        String containerId = (String) jobParameter.getValue();
                                        generateFolders(containerId, jobFolderPath);
                                    }
                                }

                                Collection<File> filesAndFolders = FileUtils.listFiles(jobFolder, null, false);
                                for(File file: filesAndFolders){
                                    if(file.exists()){
                                        if(file.isFile()){
                                            archive.addFile(file);
                                        }
                                        else if(file.isDirectory()){
                                            archive.addFolder(file);
                                        }
                                    }
                                    else{
                                        throw new IOException("File or folder with path '" + file.getAbsolutePath() + "' does not exist!");
                                    }
                                }

                                if(zipFile.exists()){
                                    chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("archiveFilePath", zipFile.getAbsolutePath());
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

                        _logger.debug("<< execute() " + Constants.STEP_PACKAGING_GENERATE_ARCHIVE + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        return jobBuilderFactory.get(Constants.JOB_PACKAGING_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(packagingValidator())
                .flow(stepCompressAssets)
                .end()
                .build();
    }

    private JobParametersValidator packagingValidator(){
        return new JobParametersValidator(){
            @Override
            @LogEntryExitExecutionTime
            public void validate(JobParameters parameters) throws JobParametersInvalidException {
                Map<String, JobParameter> parameterMap = parameters.getParameters();
                for(Map.Entry<String, JobParameter> parameterEntry: parameterMap.entrySet()){
                    if(parameterEntry.getKey().startsWith(Constants.JOB_PARAM_PACKAGING_FILE)){
                        String assetId = (String) parameterEntry.getValue().getValue();
                        if(StringUtils.isBlank(assetId)){
                            throw new JobParametersInvalidException("Job parameters have an blank asset ID!");
                        }
                    }
                    if(parameterEntry.getKey().startsWith(Constants.JOB_PARAM_PACKAGING_FOLDER)){
                        String containerId = (String) parameterEntry.getValue().getValue();
                        if(StringUtils.isBlank(containerId)){
                            throw new JobParametersInvalidException("Job parameters have an blank container ID!");
                        }
                    }
                }
            }
        };
    }

    @LogEntryExitExecutionTime
    private void generateFolders(String containerId, String parentPath) throws Exception {
        Container container = ContainerUtils.getInstance().getContainer(containersRepository, containerId);

        String folderPath = parentPath + File.separator + container.getName();
        File folder = new File(folderPath);
        boolean mkdir = folder.mkdir();
        if(mkdir){
            List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(containerId);
            for(ContainerAsset containerAsset: containerAssetsByContainerId){
                String assetId = containerAsset.getAssetId();
                Asset asset = AssetUtils.getInstance().getAsset(assetsRepository, assetId);

                File inputFile = new File(asset.getPath());
                File outputFile = new File(folderPath + File.separator + inputFile.getName());
                Files.copy(inputFile.toPath(), outputFile.toPath());
            }

            List<Container> containersByParentContainerId = containersRepository.getContainersByParentContainerId(containerId);
            for(Container childContainer : containersByParentContainerId){
                generateFolders(childContainer.getId(), folderPath);
            }
        }
        else{
            throw new IOException("Unable to create folder with path '" + folderPath + "'.");
        }
    }
}