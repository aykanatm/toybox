package com.github.murataykanat.toybox.batch.jobs;

import com.github.murataykanat.toybox.batch.utils.Constants;
import com.github.murataykanat.toybox.models.UploadFileLst;
import com.github.murataykanat.toybox.models.dbo.Asset;
import com.github.murataykanat.toybox.models.UploadFile;
import com.google.gson.Gson;
import org.apache.commons.exec.*;
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
    @Value("${importStagingThumbnailsPath}")
    private String importStagingPreviewsPath;

    @Value("${thumbnailFormat}")
    private String thumbnailFormat;
    @Value("${previewFormat}")
    private String previewFormat;

    @Value("${imagemagickExecutable}")
    private String imagemagickExecutable;

    @Value("${imagemagickThumbnailSettings}")
    private String imagemagickThumbnailSettings;
    @Value("${imagemagickPreviewSettings}")
    private String imagemagickPreviewSettings;

    @Value("${repositoryPath}")
    private String repositoryPath;

    private enum RenditionTypes{
        Thumbnail,
        Preview
    }

    @Bean
    public Job importJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory){
        Step stepGenerateAssets = stepBuilderFactory.get(Constants.STEP_IMPORT_GENERATE_ASSETS)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        List<Asset> assets = new ArrayList<>();
                        String filePathsJsonStr = (String) jobParameters.get(Constants.JOB_PARAM_UPLOADED_FILES);

                        Gson gson = new Gson();
                        UploadFileLst uploadFileLst = gson.fromJson(filePathsJsonStr, UploadFileLst.class);
                        if(uploadFileLst != null) {
                            List<UploadFile> uploadFiles = uploadFileLst.getUploadFiles();

                            for(UploadFile uploadFile: uploadFiles){
                                File inputFile = new File(uploadFile.getPath());
                                if(inputFile.exists()){
                                    if(inputFile.isFile()){
                                        String assetId = generateAssetId();
                                        String assetFolderPath = repositoryPath + File.separator + assetId;

                                        // Generate folder
                                        File assetFolder = new File(assetFolderPath);
                                        if(!assetFolder.exists()){
                                            if(!assetFolder.mkdir()){
                                                throw new Exception("Unable to create folder with path " + assetFolderPath + ".");
                                            }
                                        }

                                        // Copy asset file to repository
                                        File assetSource = inputFile;
                                        File assetDestination = new File(assetFolderPath + File.separator + assetSource.getName());
                                        FileSystemUtils.copyRecursively(assetSource, assetDestination);

                                        // Generate database entry
                                        Asset asset = new Asset();
                                        asset.setId(assetId);
                                        asset.setExtension(FilenameUtils.getExtension(assetDestination.getName()));
                                        asset.setImportDate(LocalDateTime.now().toString());
                                        asset.setImportedByUsername(uploadFile.getUsername());
                                        asset.setName(assetDestination.getName());
                                        asset.setPath(assetDestination.getAbsolutePath());
                                        asset.setPreviewPath("");
                                        asset.setThumbnailPath("");
                                        asset.setType("IMAGE");

                                        insertAsset(asset);
                                        assets.add(asset);
                                    }
                                    else{
                                        throw new Exception("File " + uploadFile.getPath() + " is not a file!");
                                    }
                                }
                                else{
                                    throw new Exception("File path " + uploadFile.getPath() + " is not valid!");
                                }
                            }
                            _logger.debug("Adding assets to job context...");
                        }
                        else{
                            throw new Exception("Upload file list is null!");
                        }

                        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("assets", assets);
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        Step stepGenerateThumbnails = stepBuilderFactory.get(Constants.STEP_IMPORT_GENERATE_THUMBNAILS)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_GENERATE_THUMBNAILS + "]");

                        Object obj = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("assets");
                        List<Asset> assets = getAssetsFromContext(obj);

                        generateRendition(assets, thumbnailFormat, imagemagickThumbnailSettings, RenditionTypes.Thumbnail);

                        _logger.debug("<< execute() " + Constants.STEP_IMPORT_GENERATE_THUMBNAILS + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        Step stepGeneratePreviews = stepBuilderFactory.get(Constants.STEP_IMPORT_GENERATE_PREVIEWS)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_GENERATE_PREVIEWS + "]");

                        Object obj = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("assets");
                        List<Asset> assets = getAssetsFromContext(obj);

                        generateRendition(assets, previewFormat, imagemagickPreviewSettings, RenditionTypes.Preview);

                        _logger.debug("<< execute() " + Constants.STEP_IMPORT_GENERATE_PREVIEWS + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        return jobBuilderFactory.get(Constants.JOB_IMPORT_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(importValidator())
                .flow(stepGenerateAssets)
                .next(stepGenerateThumbnails)
                .next(stepGeneratePreviews)
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
                UploadFileLst uploadedFileList = gson.fromJson(filePathsJsonStr, UploadFileLst.class);
                if(uploadedFileList != null){
                    List<UploadFile> uploadFiles = uploadedFileList.getUploadFiles();
                    for(UploadFile uploadFile: uploadFiles){
                        File file = new File(uploadFile.getPath());
                        if(!file.exists()){
                            throw new JobParametersInvalidException("File '" + uploadFile.getPath() + "' did not exist or was not readable.");
                        }
                    }
                }
                else{
                    throw new JobParametersInvalidException("Uploaded file list is null!");
                }

                _logger.debug("<< validate()");
            }
        };
    }

    private void createFolder(String assetFolderPath, String assetRenditionPath) throws Exception {
        _logger.debug("createAssetFolders() >>");
        try{
            String[] paths = new String[]{assetFolderPath, assetRenditionPath};
            for(String path: paths){
                File directory = new File(path);
                if(!directory.exists()){
                    _logger.debug("Creating folder '" + path + "'...");
                    if(!directory.mkdir()){
                        throw new Exception("Unable to create folder with path " + path + ".");
                    }
                }
            }
        }
        catch (Exception e){
            throw e;
        }

        _logger.debug("<< createAssetFolders()");
    }

    private void updateAssetRendition(String assetId, String renditionPath, RenditionTypes renditionType) throws Exception {
        _logger.debug("updateAsset() >>");
        try {
            if(renditionType == RenditionTypes.Thumbnail){
                jdbcTemplate.update("UPDATE assets SET asset_thumbnail_path=? WHERE asset_id=?",new Object[]{renditionPath, assetId});

            }
            else if(renditionType == RenditionTypes.Preview){
                jdbcTemplate.update("UPDATE assets SET asset_preview_path=? WHERE asset_id=?",new Object[]{renditionPath, assetId});
            }
            else{
                throw new Exception("Unknown rendition type!");
            }
        }
        catch (Exception e){
            throw e;
        }
        _logger.debug("<< updateAsset()");
    }

    private void insertAsset(Asset asset){
        _logger.debug("insertAsset() >>");

        try{
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
        }
        catch (Exception e){
            throw e;
        }

        _logger.debug("<< insertAsset()");
    }

    private String generateAssetId(){
        _logger.debug("generateAssetId() >>");
        try{
            String assetId = RandomStringUtils.randomAlphanumeric(Constants.ASSET_ID_LENGTH);
            if(isAssetIdValid(assetId)){
                _logger.debug("<< generateAssetId() [" + assetId + "]");
                return assetId;
            }
            return generateAssetId();
        }
        catch (Exception e){
            throw e;
        }
    }

    private boolean isAssetIdValid(String assetId){
        _logger.debug("isAssetIdValid() >> [" + assetId + "]");
        boolean result = false;

        try{
            List<Asset> assets = jdbcTemplate.query("SELECT asset_id FROM assets WHERE asset_id=?", new Object[]{assetId}, (rs, rowNum) -> new Asset(rs.getString("asset_id")));
            if(assets != null){
                if(assets.size() > 0){
                    _logger.debug("<< isAssetIdValid() [" + false + "]");
                    result = false;
                }
                else{
                    _logger.debug("<< isAssetIdValid() [" + true + "]");
                    result = true;
                }
            }
        }
        catch (Exception e){
            throw e;
        }

        _logger.debug("<< isAssetIdValid() [" + true + "]");
        return result;
    }

    private void generateRendition(List<Asset> assets, String format, String imagemagickRenditionSettings, RenditionTypes renditionType) throws Exception {
        _logger.debug("generateRendition() >>");

        try{
            for(Asset asset: assets){
                File inputFile = new File(asset.getPath());

                String assetFolderPath = repositoryPath + File.separator + asset.getId();

                File outputFile;
                if(renditionType == RenditionTypes.Preview){
                    String assetPreviewPath = repositoryPath + File.separator + asset.getId() + File.separator + "preview";
                    createFolder(assetFolderPath, assetPreviewPath);
                    outputFile = new File(assetPreviewPath + File.separator + asset.getId() + "." + format);
                }
                else if(renditionType == RenditionTypes.Thumbnail){
                    String assetThumbnailPath = repositoryPath + File.separator + asset.getId() + File.separator + "thumbnail";
                    createFolder(assetFolderPath, assetThumbnailPath);
                    outputFile = new File(assetThumbnailPath + File.separator + asset.getId() + "." + format);
                }
                else{
                    throw new Exception("Unknown rendition type!");
                }

                // Generate rendition
                CommandLine cmdLine = new CommandLine(imagemagickExecutable);
                cmdLine.addArgument("${inputFile}");
                String[] arguments = imagemagickRenditionSettings.split("\\s+");
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
                    String errorMessage = "ImageMagick failed to generate rendition of the file '" + inputFile.getAbsolutePath()
                            + "'. " + resultHandler.getException().getLocalizedMessage();
                    throw new Exception(errorMessage);
                }
                else{
                    _logger.debug("ImageMagick successfully generated the rendition '" + outputFile.getAbsolutePath());
                    updateAssetRendition(asset.getId(), outputFile.getAbsolutePath(), renditionType);
                }
            }
        }
        catch (Exception e){
            throw e;
        }

        _logger.debug("<< generateRendition()");
    }

    private List<Asset> getAssetsFromContext(Object obj) throws Exception {
        _logger.debug("getAssetsFromContext() >>");
        if(obj instanceof List){
            if(((List) obj).size() > 0){
                if(((List) obj).get(0) instanceof Asset){
                    _logger.debug("<< getAssetsFromContext()");
                    return (List<Asset>) obj;
                }
                else{
                    throw new Exception("Objects that are in the list are not of type Asset.");
                }
            }
            else{
                throw new Exception("List is empty and there is no way to check if the contents are of type Asset.");
            }
        }
        else{
            throw new Exception("Assets object is not of type List.");
        }
    }
}
