package com.github.murataykanat.toybox.batch.jobs;

import com.github.murataykanat.toybox.batch.utils.Constants;
import com.github.murataykanat.toybox.models.RenditionProperties;
import com.github.murataykanat.toybox.models.dbo.Asset;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.jodconverter.LocalConverter;
import org.jodconverter.filter.text.PageSelectorFilter;
import org.jodconverter.office.*;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.file.Files;
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

    @Value("${imageThumbnailFormat}")
    private String imageThumbnailFormat;
    @Value("${videoThumbnailFormat}")
    private String videoThumbnailFormat;
    @Value("${audioThumbnailFormat}")
    private String audioThumbnailFormat;

    @Value("${imagePreviewFormat}")
    private String imagePreviewFormat;
    @Value("${videoPreviewFormat}")
    private String videoPreviewFormat;
    @Value("${audioPreviewFormat}")
    private String audioPreviewFormat;

    @Value("${imagemagickExecutable}")
    private String imagemagickExecutable;

    @Value("${imagemagickThumbnailSettings}")
    private String imagemagickThumbnailSettings;
    @Value("${imagemagickEpsThumbnailSettings}")
    private String imagemagickEpsThumbnailSettings;
    @Value("${imagemagickPdfThumbnailSettings}")
    private String imagemagickPdfThumbnailSettings;

    @Value("${imagemagickPreviewSettings}")
    private String imagemagickPreviewSettings;
    @Value("${imagemagickEpsPreviewSettings}")
    private String imagemagickEpsPreviewSettings;
    @Value("${imagemagickPdfPreviewSettings}")
    private String imagemagickPdfPreviewSettings;
    @Value("${imagemagickTimeout}")
    private String imagemagickTimeout;

    @Value("${gifsicleExecutable}")
    private String gifsicleExecutable;

    @Value("${gifsicleThumbnailSettings}")
    private String gifsicleThumbnailSettings;
    @Value("${gifsiclePreviewSettings}")
    private String gifsiclePreviewSettings;
    @Value("${gifsicleTimeout}")
    private String gifsicleTimeout;

    @Value("${ffmpegExecutable}")
    private String ffmpegExecutable;
    @Value("${ffmpegVideoPreviewSettings}")
    private String ffmpegVideoPreviewSettings;
    @Value("${ffmpegVideoThumbnailSettings}")
    private String ffmpegVideoThumbnailSettings;
    @Value("${ffmpegAudioPreviewSettings}")
    private String ffmpegAudioPreviewSettings;
    @Value("${ffmpegAudioThumbnailSettings}")
    private String ffmpegAudioThumbnailSettings;
    @Value("${ffmpegTimeout}")
    private String ffmpegTimeout;

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
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_GENERATE_ASSETS + "]");
                        Tika tika = new Tika();
                        List<Asset> assets = new ArrayList<>();

                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        String username = (String) jobParameters.get(Constants.JOB_PARAM_USERNAME);

                        for(Map.Entry<String, Object> jobParameter: jobParameters.entrySet()){
                            if(jobParameter.getKey().startsWith(Constants.JOB_PARAM_UPLOADED_FILE)){
                                String filePath = (String) jobParameter.getValue();
                                File file = new File(filePath);
                                if(file.exists()){
                                    if(file.isFile()){
                                        String assetId = generateAssetId();
                                        String assetFolderPath = repositoryPath + File.separator + assetId;

                                        // Generate folder
                                        File assetFolder = new File(assetFolderPath);
                                        if(!assetFolder.exists() && !assetFolder.mkdir()){
                                            throw new IOException("Unable to create folder with path " + assetFolderPath + ".");
                                        }

                                        // Copy asset file to repository
                                        File assetSource = file;
                                        File assetDestination = new File(assetFolderPath + File.separator + assetSource.getName());
                                        FileSystemUtils.copyRecursively(assetSource, assetDestination);

                                        String assetMimeType = "UNKNOWN";
                                        if(assetDestination.exists() && assetDestination.isFile()){
                                            assetMimeType = tika.detect(assetDestination);
                                            _logger.debug("Asset mime type: " + assetMimeType);
                                        }

                                        // Generate database entry
                                        Asset asset = new Asset();
                                        asset.setId(assetId);
                                        asset.setExtension(FilenameUtils.getExtension(assetDestination.getName()));
                                        asset.setImportDate(LocalDateTime.now().toString());
                                        asset.setImportedByUsername(username);
                                        asset.setName(assetDestination.getName());
                                        asset.setPath(assetDestination.getAbsolutePath());
                                        asset.setPreviewPath("");
                                        asset.setThumbnailPath("");
                                        asset.setType(assetMimeType);

                                        insertAsset(asset);
                                        assets.add(asset);
                                    }
                                    else{
                                        throw new IOException("File " + filePath + " is not a file!");
                                    }
                                }
                                else{
                                    throw new FileNotFoundException("File path " + filePath + " is not valid!");
                                }
                            }
                        }

                        _logger.debug("Adding assets to job context...");
                        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("assets", assets);

                        _logger.debug("<< execute() [" + Constants.STEP_IMPORT_GENERATE_ASSETS + "]");
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

                        generateRendition(assets, RenditionTypes.Thumbnail);

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

                        generateRendition(assets, RenditionTypes.Preview);

                        _logger.debug("<< execute() " + Constants.STEP_IMPORT_GENERATE_PREVIEWS + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        Step stepDeleteTempFiles = stepBuilderFactory.get(Constants.STEP_IMPORT_DELETE_TEMP_FILES)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_IMPORT_DELETE_TEMP_FILES + "]");

                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        for(Map.Entry<String, Object> jobParameter: jobParameters.entrySet()){
                            if(jobParameter.getKey().startsWith(Constants.JOB_PARAM_UPLOADED_FILE)){
                                String filePath = (String) jobParameter.getValue();
                                File file = new File(filePath);
                                if(file.exists()){
                                    File parentFolder = file.getParentFile();
                                    if(parentFolder.exists()){
                                        if(parentFolder.isDirectory()){
                                            FileUtils.deleteDirectory(parentFolder);
                                            break;
                                        }
                                        else{
                                            throw new IOException("Directory " + parentFolder.getAbsolutePath() + " is not a directory!");
                                        }
                                    }
                                    else{
                                        throw new FileNotFoundException("File path " + parentFolder.getAbsolutePath() + " is not valid!");
                                    }
                                }
                                else{
                                    throw new FileNotFoundException("File path " + filePath + " is not valid!");
                                }
                            }
                        }

                        _logger.debug("<< execute() " + Constants.STEP_IMPORT_DELETE_TEMP_FILES + "]");
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
                .next(stepDeleteTempFiles)
                .end()
                .build();
    }

    private JobParametersValidator importValidator(){
        return new JobParametersValidator(){
            @Override
            public void validate(JobParameters parameters) throws JobParametersInvalidException {
                _logger.debug("validate() >>");

                Map<String, JobParameter> parameterMap = parameters.getParameters();
                for(Map.Entry<String, JobParameter> parameterEntry: parameterMap.entrySet()){
                    if(parameterEntry.getKey().startsWith(Constants.JOB_PARAM_UPLOADED_FILE)){
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

    private void createFolder(String assetFolderPath, String assetRenditionPath) throws IOException {
        _logger.debug("createAssetFolders() >>");
        String[] paths = new String[]{assetFolderPath, assetRenditionPath};
        for(String path: paths){
            File directory = new File(path);
            if(!directory.exists()){
                _logger.debug("Creating folder '" + path + "'...");
                if(!directory.mkdir()){
                    throw new IOException("Unable to create folder with path " + path + ".");
                }
            }
        }

        _logger.debug("<< createAssetFolders()");
    }

    private void updateAssetRendition(String assetId, String renditionPath, RenditionTypes renditionType) {
        _logger.debug("updateAsset() >>");
        if(renditionType == RenditionTypes.Thumbnail){
            jdbcTemplate.update("UPDATE assets SET asset_thumbnail_path=? WHERE asset_id=?",new Object[]{renditionPath, assetId});

        }
        else if(renditionType == RenditionTypes.Preview){
            jdbcTemplate.update("UPDATE assets SET asset_preview_path=? WHERE asset_id=?",new Object[]{renditionPath, assetId});
        }
        else{
            throw new IllegalArgumentException("Unknown rendition type!");
        }
        _logger.debug("<< updateAsset()");
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
        boolean result = false;

        List<Asset> assets = jdbcTemplate.query("SELECT asset_id FROM assets WHERE asset_id=?", new Object[]{assetId}, (rs, rowNum) -> new Asset(rs.getString("asset_id")));
        if(assets != null){
            if(!assets.isEmpty()){
                _logger.debug("<< isAssetIdValid() [" + false + "]");
                result = false;
            }
            else{
                _logger.debug("<< isAssetIdValid() [" + true + "]");
                result = true;
            }
        }

        _logger.debug("<< isAssetIdValid() [" + true + "]");
        return result;
    }

    private RenditionProperties getRenditionProperties(Asset asset, String assetRenditionPath, RenditionTypes renditionType) {
        RenditionProperties renditionProperties = new RenditionProperties();

        String fileFormat;
        String gifsicleSettings;
        String imagemagickSettings;
        String imagemagickEpsSettings;
        String imagemagickPdfSettings;
        String ffmpegVideoSettings;
        String ffmpegAudioSettings;

        if(renditionType == RenditionTypes.Thumbnail){
            if(asset.getType().startsWith(Constants.VIDEO_MIME_TYPE_PREFIX)){
                fileFormat = videoThumbnailFormat;
            }
            else{
                fileFormat = imageThumbnailFormat;
            }

            gifsicleSettings = gifsicleThumbnailSettings;
            imagemagickSettings = imagemagickThumbnailSettings;
            imagemagickEpsSettings = imagemagickEpsThumbnailSettings;
            imagemagickPdfSettings = imagemagickPdfThumbnailSettings;
            ffmpegVideoSettings = ffmpegVideoThumbnailSettings;
            ffmpegAudioSettings = ffmpegAudioThumbnailSettings;
        }
        else if(renditionType == RenditionTypes.Preview){
            if(asset.getType().startsWith(Constants.VIDEO_MIME_TYPE_PREFIX)){
                fileFormat = videoPreviewFormat;
            }
            else if(asset.getType().startsWith(Constants.AUIDO_MIME_TYPE_PREFIX)){
                fileFormat = audioPreviewFormat;
            }
            else{
                fileFormat = imagePreviewFormat;
            }

            gifsicleSettings = gifsiclePreviewSettings;
            imagemagickSettings = imagemagickPreviewSettings;
            imagemagickEpsSettings = imagemagickEpsPreviewSettings;
            imagemagickPdfSettings = imagemagickPdfPreviewSettings;
            ffmpegVideoSettings = ffmpegVideoPreviewSettings;
            ffmpegAudioSettings = ffmpegAudioPreviewSettings;
        }
        else{
            throw new IllegalArgumentException("Unknown rendition type!");
        }

        String assetMimeType = asset.getType();
        if(assetMimeType.equalsIgnoreCase(Constants.IMAGE_MIME_TYPE_GIF)){
            renditionProperties.setRenditionSettings(gifsicleSettings);
            renditionProperties.setOutputFile(new File(assetRenditionPath + File.separator + asset.getId() + ".gif"));
        }
        else if(assetMimeType.startsWith(Constants.IMAGE_MIME_TYPE_PREFIX)){
            renditionProperties.setRenditionSettings(imagemagickSettings);
            renditionProperties.setOutputFile(new File(assetRenditionPath + File.separator + asset.getId() + "." + fileFormat));
        }
        else if(assetMimeType.equalsIgnoreCase(Constants.IMAGE_MIME_TYPE_EPS)){
            renditionProperties.setRenditionSettings(imagemagickEpsSettings);
            renditionProperties.setOutputFile(new File(assetRenditionPath + File.separator + asset.getId() + "." + fileFormat));
        }
        else if(assetMimeType.equalsIgnoreCase(Constants.FILE_MIME_TYPE_PDF)){
            renditionProperties.setRenditionSettings(imagemagickPdfSettings);
            renditionProperties.setOutputFile(new File(assetRenditionPath + File.separator + asset.getId() + "." + fileFormat));
        }
        else if(assetMimeType.startsWith(Constants.VIDEO_MIME_TYPE_PREFIX)){
            renditionProperties.setRenditionSettings(ffmpegVideoSettings);
            renditionProperties.setOutputFile(new File(assetRenditionPath + File.separator + asset.getId() + "." + fileFormat));
        }
        else if(assetMimeType.startsWith(Constants.AUIDO_MIME_TYPE_PREFIX)){
            renditionProperties.setRenditionSettings(ffmpegAudioSettings);
            renditionProperties.setOutputFile(new File(assetRenditionPath + File.separator + asset.getId() + "." + fileFormat));
        }
        else if(assetMimeType.equalsIgnoreCase(Constants.FILE_MIME_TYPE_PPTX)
                || assetMimeType.equalsIgnoreCase(Constants.FILE_MIME_TYPE_PPT)
                || assetMimeType.equalsIgnoreCase(Constants.FILE_MIME_TYPE_XLSX)
                || assetMimeType.equalsIgnoreCase(Constants.FILE_MIME_TYPE_XLS)
                || assetMimeType.equalsIgnoreCase(Constants.FILE_MIME_TYPE_DOCX)
                || assetMimeType.equalsIgnoreCase(Constants.FILE_MIME_TYPE_DOC)){
            renditionProperties.setRenditionSettings(imagemagickSettings);
            renditionProperties.setOutputFile(new File(assetRenditionPath + File.separator + asset.getId() + "." + fileFormat));
        }

        return renditionProperties;
    }

    private void runExecutable(Asset asset, File inputFile, File outputFile, String executable, String timeout, String renditionSettings, RenditionTypes renditionType, String executableName) throws IOException, InterruptedException {
        CommandLine cmdLine = new CommandLine(executable);

        if(executableName.equalsIgnoreCase(Constants.FFMPEG)){
            cmdLine.addArgument("-i");
        }

        cmdLine.addArgument("${inputFile}");

        if(StringUtils.isNotBlank(renditionSettings)){
            String[] arguments = renditionSettings.split("\\s+");
            for(String argument: arguments){
                cmdLine.addArgument(argument);
            }
        }

        cmdLine.addArgument("${outputFile}");
        HashMap map = new HashMap();
        map.put("inputFile", inputFile);
        map.put("outputFile", outputFile);
        cmdLine.setSubstitutionMap(map);

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        long timeoutValue = Long.parseLong(timeout);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutValue);
        Executor executor = new DefaultExecutor();
        executor.setExitValue(1);
        executor.setWatchdog(watchdog);
        _logger.debug("Executing command: " + cmdLine.toString());
        executor.execute(cmdLine, resultHandler);

        resultHandler.waitFor();

        int exitValue = resultHandler.getExitValue();
        if(exitValue != 0){
            if(renditionType == RenditionTypes.Thumbnail && asset.getType().startsWith(Constants.AUIDO_MIME_TYPE_PREFIX)){
                String utilityErrorMessage = resultHandler.getException() != null ? resultHandler.getException().getLocalizedMessage() : "This audio file may have no album art to retrieve. If you would like to see a thumbnail please include an album art.";
                String warningMessage = executableName + " failed to generate rendition of the file '" + inputFile.getAbsolutePath()
                        + "'. " + utilityErrorMessage;
                _logger.warn(warningMessage);
            }
            else{
                String utilityErrorMessage = resultHandler.getException() != null ? resultHandler.getException().getLocalizedMessage() : "";
                String errorMessage = executableName + " failed to generate rendition of the file '" + inputFile.getAbsolutePath()
                        + "'. " + utilityErrorMessage;
                _logger.error(errorMessage);
            }
        }
        else{
            _logger.info(executableName + " successfully generated the rendition '" + outputFile.getAbsolutePath());
            updateAssetRendition(asset.getId(), outputFile.getAbsolutePath(), renditionType);
        }
    }
    private void generateRendition(List<Asset> assets, RenditionTypes renditionType) throws IOException, InterruptedException {
        _logger.debug("generateRendition() >>");
        for(Asset asset: assets){
            File inputFile = new File(asset.getPath());
            if(inputFile.exists()){
                if(asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_PDF) || asset.getType().equalsIgnoreCase(Constants.IMAGE_MIME_TYPE_PHOTOSHOP)){
                    // If we are creating renditions of a PDF file, we only use the first page
                    // If we are creating renditions of a PSB file, we get the top layer
                    inputFile = new File(asset.getPath() + "[0]");
                }

                String assetFolderPath = repositoryPath + File.separator + asset.getId();
                File outputFile;
                String renditionSettings;
                String assetPreviewPath = null;
                String assetThumbnailPath = null;

                if(renditionType == RenditionTypes.Preview){
                    assetPreviewPath = repositoryPath + File.separator + asset.getId() + File.separator + "preview";
                    createFolder(assetFolderPath, assetPreviewPath);

                    RenditionProperties renditionProperties = getRenditionProperties(asset, assetPreviewPath, renditionType);
                    outputFile = renditionProperties.getOutputFile();
                    renditionSettings = renditionProperties.getRenditionSettings();
                }
                else if(renditionType == RenditionTypes.Thumbnail){
                    assetThumbnailPath = repositoryPath + File.separator + asset.getId() + File.separator + "thumbnail";
                    createFolder(assetFolderPath, assetThumbnailPath);

                    RenditionProperties renditionProperties = getRenditionProperties(asset, assetThumbnailPath, renditionType);
                    outputFile = renditionProperties.getOutputFile();
                    renditionSettings = renditionProperties.getRenditionSettings();
                }
                else{
                    throw new IllegalArgumentException("Unknown rendition type!");
                }

                if(StringUtils.isNotBlank(renditionSettings)){
                    // Generate rendition
                    if(asset.getType().equalsIgnoreCase(Constants.IMAGE_MIME_TYPE_GIF)){
                        runExecutable(asset, inputFile, outputFile, gifsicleExecutable, gifsicleTimeout, renditionSettings, renditionType,Constants.GIFSICLE);
                    }
                    else if(asset.getType().startsWith(Constants.IMAGE_MIME_TYPE_PREFIX)
                            || asset.getType().equalsIgnoreCase(Constants.IMAGE_MIME_TYPE_EPS)
                            || asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_PDF)
                            || asset.getType().equalsIgnoreCase(Constants.IMAGE_MIME_TYPE_PHOTOSHOP)){
                        runExecutable(asset, inputFile, outputFile, imagemagickExecutable, imagemagickTimeout, renditionSettings, renditionType, Constants.IMAGEMAGICK);
                    }
                    else if(asset.getType().startsWith(Constants.VIDEO_MIME_TYPE_PREFIX) || asset.getType().startsWith(Constants.AUIDO_MIME_TYPE_PREFIX)){
                        runExecutable(asset, inputFile, outputFile, ffmpegExecutable, ffmpegTimeout, renditionSettings, renditionType, Constants.FFMPEG);
                        
                        // Since we can only generate video thumbnails as JPEG, let's convert them to PNG
                        if(renditionType == RenditionTypes.Thumbnail){
                            if(outputFile.exists()){
                                if(StringUtils.isNotBlank(assetThumbnailPath)){
                                    File pngOutput = new File(assetThumbnailPath + File.separator + asset.getId() + "." + imageThumbnailFormat);
                                    runExecutable(asset, outputFile, pngOutput, imagemagickExecutable, imagemagickTimeout, null, renditionType, Constants.IMAGEMAGICK);
                                    Files.delete(outputFile.toPath());
                                }
                                else{
                                    throw new InvalidObjectException("Asset thumbnail path is blank!");
                                }
                            }
                            else{
                                _logger.warn("No thumbnail was generated for " + asset.getName() + ". Skipping thumbnail format conversion...");
                            }
                        }
                    }
                    else if(asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_DOCX)
                            || asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_DOC)
                            || asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_XLSX)
                            || asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_XLS)
                            || asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_PPTX)
                            || asset.getType().equalsIgnoreCase(Constants.FILE_MIME_TYPE_PPT)){
                        LocalOfficeManager officeManager = LocalOfficeManager.install();

                        try{
                            officeManager.start();

                            PageSelectorFilter selectorFilter = new PageSelectorFilter(1);

                            if(outputFile != null){
                                if(!outputFile.exists()){
                                    _logger.debug("Creating file " + outputFile.getAbsolutePath() + "...");
                                    boolean newFile = outputFile.createNewFile();
                                    if(newFile){
                                        _logger.debug("Generating office rendition...");
                                        LocalConverter
                                                .builder()
                                                .filterChain(selectorFilter)
                                                .build()
                                                .convert(inputFile)
                                                .to(outputFile)
                                                .execute();

                                        File pngOutput;
                                        if(renditionType == RenditionTypes.Thumbnail){
                                            pngOutput = new File(assetThumbnailPath + File.separator + asset.getId() + "." + imageThumbnailFormat);
                                        }
                                        else if(renditionType == RenditionTypes.Preview){
                                            pngOutput = new File(assetPreviewPath + File.separator + asset.getId() + "." + imageThumbnailFormat);
                                        }
                                        else{
                                            throw new IllegalArgumentException("Unknown rendition type!");
                                        }

                                        runExecutable(asset, outputFile, pngOutput, imagemagickExecutable, imagemagickTimeout, renditionSettings, renditionType, Constants.IMAGEMAGICK);
                                    }
                                    else{
                                        throw new IOException("Unable to create file " + outputFile.getAbsolutePath() + ".");
                                    }
                                }
                                else{
                                    throw new IOException("Output file " + outputFile.getAbsolutePath() + " already exists!");
                                }
                            }
                            else{
                                throw new InvalidObjectException("Output file is null!");
                            }
                        }
                        catch (Exception e){
                            String errorMessage = "Office failed to generate the rendition of the file " + inputFile.getAbsolutePath() + ". " + e.getLocalizedMessage();
                            _logger.error(errorMessage, e);
                        }
                        finally {
                            LocalOfficeUtils.stopQuietly(officeManager);
                        }
                    }
                }
                else{
                    _logger.debug("There is no rendition settings to apply. Skipping...");
                }
            }
            else{
                throw new FileNotFoundException("File " + asset.getPath() + " does not exist!");
            }
        }

        _logger.debug("<< generateRendition()");
    }

    private List<Asset> getAssetsFromContext(Object obj) {
        _logger.debug("getAssetsFromContext() >>");
        if(obj instanceof List){
            if(!((List) obj).isEmpty()){
                if(((List) obj).get(0) instanceof Asset){
                    _logger.debug("<< getAssetsFromContext()");
                    return (List<Asset>) obj;
                }
                else{
                    throw new IllegalArgumentException("Objects that are in the list are not of type Asset.");
                }
            }
            else{
                throw new IllegalArgumentException("List is empty and there is no way to check if the contents are of type Asset.");
            }
        }
        else{
            throw new IllegalArgumentException("Assets object is not of type List.");
        }
    }
}
