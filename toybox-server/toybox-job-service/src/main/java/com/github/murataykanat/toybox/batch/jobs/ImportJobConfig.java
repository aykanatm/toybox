package com.github.murataykanat.toybox.batch.jobs;

import com.github.murataykanat.toybox.batch.utils.Constants;
import com.github.murataykanat.toybox.models.dbo.Asset;
import com.github.murataykanat.toybox.models.UploadFile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RefreshScope
@Configuration
@EnableBatchProcessing
public class ImportJobConfig {
    private static final Log _logger = LogFactory.getLog(ImportJobConfig.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${importStagingThumbnailsPath}")
    private String importStagingThumbnailsPath;

    @Value("${thumbnailFormat}")
    private String thumbnailFormat;
    @Value("${imagemagickExecutable}")
    private String imagemagickExecutable;
    @Value("${imagemagickThumbnailSettings}")
    private String imagemagickThumbnailSettings;

    @Value("${repositoryPath}")
    private String repositoryPath;

    private Map<String, UploadFile> thumbnailsToUploadedFilesMap;

    // TODO:
    // Implement exception handling

    @Bean
    public Job importJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory){
        Step stepGenerateThumbnails = stepBuilderFactory.get(Constants.STEP_IMPORT_GENERATE_THUMBNAILS)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_GENERATE_THUMBNAILS + "]");
                        thumbnailsToUploadedFilesMap = new HashMap<>();

                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        for(Map.Entry<String, Object> entry: jobParameters.entrySet()){
                            if(entry.getKey().equalsIgnoreCase(Constants.JOB_PARAM_UPLOADED_FILES)){
                                String filePathsJsonStr = (String) entry.getValue();

                                Gson gson = new Gson();
                                Type listType = new TypeToken<ArrayList<UploadFile>>(){}.getType();
                                ArrayList<UploadFile> uploadedFiles = gson.fromJson(filePathsJsonStr, listType);

                                for(UploadFile uploadFile: uploadedFiles){
                                    File inputFile = new File(uploadFile.getPath());
                                    String thumbnailName = "thumb_" + FilenameUtils.removeExtension(inputFile.getName()) + "." + thumbnailFormat;
                                    File outputFile = new File(importStagingThumbnailsPath + File.separator + thumbnailName);

                                    // Generate thumbnail
                                    CommandLine cmdLine = new CommandLine(imagemagickExecutable);
                                    cmdLine.addArgument("${inputFile}");
                                    String[] arguments = imagemagickThumbnailSettings.split("\\s+");
                                    for(String argument: arguments){
                                        cmdLine.addArgument(argument);
                                    }
                                    cmdLine.addArgument("${outputFile}");
                                    HashMap map = new HashMap();
                                    map.put("inputFile", inputFile);
                                    map.put("outputFile", outputFile);
                                    cmdLine.setSubstitutionMap(map);

                                    DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

                                    ExecuteWatchdog watchdog = new ExecuteWatchdog(Constants.IMAGEMAGICK_TIMEOUT);
                                    Executor executor = new DefaultExecutor();
                                    executor.setExitValue(1);
                                    executor.setWatchdog(watchdog);
                                    _logger.debug("Executing command: " + cmdLine.toString());
                                    executor.execute(cmdLine, resultHandler);

                                    resultHandler.waitFor();

                                    int exitValue = resultHandler.getExitValue();
                                    if(exitValue != 0){
                                        _logger.error("ImageMagick failed to generate thumbnail of the file '" + inputFile.getAbsolutePath()
                                                + "'. " + resultHandler.getException().getLocalizedMessage(), resultHandler.getException());
                                    }
                                    else{
                                        _logger.debug("ImageMagick successfully generated the thumbnail '" + outputFile.getAbsolutePath());
                                        thumbnailsToUploadedFilesMap.put(outputFile.getAbsolutePath(), uploadFile);
                                    }
                                }
                            }
                        }
                        _logger.debug("<< execute() " + Constants.STEP_IMPORT_GENERATE_THUMBNAILS + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        // TODO:
        // Add generate previews step

        Step stepGenerateAssets = stepBuilderFactory.get(Constants.STEP_IMPORT_GENERATE_ASSET)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_GENERATE_ASSET + "]");
                        for(Map.Entry<String, UploadFile> entry: thumbnailsToUploadedFilesMap.entrySet()){
                            String assetId = generateAssetId();

                            String assetFolderPath = repositoryPath + File.separator + assetId;
                            String assetThumbnailPath = repositoryPath + File.separator + assetId + File.separator + "thumbnail";

                            // Create asset folders
                            createAssetFolders(assetFolderPath, assetThumbnailPath);

                            // Copy the thumbnail to repository
                            File thumbnailSource = new File(entry.getKey());
                            File thumbnailDestination = new File(assetThumbnailPath + File.separator + assetId + "." + thumbnailFormat);
                            FileSystemUtils.copyRecursively(thumbnailSource, thumbnailDestination);

                            // Copy asset file to repository
                            File assetSource = new File(entry.getValue().getPath());
                            File assetDestination = new File(assetFolderPath + File.separator + assetSource.getName());
                            FileSystemUtils.copyRecursively(assetSource, assetDestination);

                            // Generate database entry
                            Asset asset = new Asset();
                            asset.setId(assetId);
                            asset.setExtension(FilenameUtils.getExtension(assetDestination.getName()));
                            asset.setImportDate(LocalDateTime.now().toString());
                            asset.setImportedByUsername(entry.getValue().getUsername());
                            asset.setName(assetDestination.getName());
                            asset.setPath(assetDestination.getAbsolutePath());
                            asset.setPreviewPath("");
                            asset.setThumbnailPath(thumbnailDestination.getAbsolutePath());
                            asset.setType("IMAGE");

                            insertAsset(asset);
                        }

                        _logger.debug("<< execute() [" + Constants.STEP_IMPORT_GENERATE_ASSET + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        Step deleteTempFiles = stepBuilderFactory.get(Constants.STEP_IMPORT_DELETE_TEMP_FILES)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_DELETE_TEMP_FILES + "]");
                        for(Map.Entry<String, UploadFile> entry: thumbnailsToUploadedFilesMap.entrySet()){
                            // Delete generated thumbnail
                            _logger.debug("Deleting temp file '" + entry.getKey() + "'...");
                            FileUtils.forceDelete(new File(entry.getKey()));
                            // Delete uploaded file
                            _logger.debug("Deleting temp file '" + entry.getValue().getPath() + "'...");
                            FileUtils.forceDelete(new File(entry.getValue().getPath()));
                        }
                        _logger.debug("<< execute() [" + Constants.STEP_IMPORT_DELETE_TEMP_FILES + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        return jobBuilderFactory.get(Constants.JOB_IMPORT_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(importValidator())
                .flow(stepGenerateThumbnails)
                .next(stepGenerateAssets)
                .next(deleteTempFiles)
                .end()
                .build();
    }

    private JobParametersValidator importValidator(){
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

    private void createAssetFolders(String assetFolderPath, String assetThumbnailPath){
        _logger.debug("createAssetFolders() >>");
        String[] paths = new String[]{assetFolderPath, assetThumbnailPath};
        for(String path: paths){
            File directory = new File(path);
            if(!directory.exists()){
                _logger.debug("Creating folder '" + directory.getAbsolutePath() + "'...");
                directory.mkdir();
            }
        }

        _logger.debug("<< createAssetFolders()");
    }

    private void insertAsset(Asset asset){
        _logger.debug("insertAsset() >>");

        _logger.debug("Asset:");
        _logger.debug("Asset ID: " + asset.getId());
        _logger.debug("Asset Extension: " + asset.getExtension());
        _logger.debug("Asset Imported By Username: " + asset.getImportedByUsername());
        _logger.debug("Asset Name: " + asset.getName());
        _logger.debug("Asset Path: " + asset.getPath());
        _logger.debug("Asset Preview Path: " + asset.getPreviewPath());
        _logger.debug("Asset Thumbnail Path: " + asset.getThumbnailPath());
        _logger.debug("Asset Type: " + asset.getType());
        _logger.debug("Asset Import Date: " + asset.getImportDate());

        _logger.debug("Inserting asset into the database...");

        jdbcTemplate.update("INSERT INTO assets(asset_id, asset_extension, asset_imported_by_username, " +
                        "asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, asset_import_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                asset.getId(), asset.getName(), asset.getImportedByUsername(), asset.getName(), asset.getPath(),
                asset.getPreviewPath(), asset.getThumbnailPath(), asset.getType(), asset.getImportDate());
        _logger.debug("<< insertAsset()");
    }

    private String generateAssetId(){
        _logger.debug("generateAssetId() >>");
        String assetId = RandomStringUtils.randomAlphanumeric(Constants.ASSET_ID_LENGTH);
        if(isAssetIdValid(assetId)){
            _logger.debug("<< generateAssetId() [" + assetId + "]");
            return assetId;
        }
        return generateAssetId();
    }

    private boolean isAssetIdValid(String assetId){
        _logger.debug("isAssetIdValid() >> [" + assetId + "]");
        List<Asset> result = jdbcTemplate.query("SELECT asset_id FROM assets WHERE asset_id=?", new Object[]{assetId}, (rs, rowNum) -> new Asset(rs.getString("asset_id")));
        if(result != null){
            if(result.size() > 0){
                _logger.debug("<< isAssetIdValid() [" + false + "]");
                return false;
            }
            else{
                _logger.debug("<< isAssetIdValid() [" + true + "]");
                return true;
            }
        }
        _logger.debug("<< isAssetIdValid() [" + true + "]");
        return true;
    }
}
