package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import com.github.murataykanat.toybox.schema.asset.UpdateAssetRequest;
import com.github.murataykanat.toybox.schema.job.JobResponse;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AssetUtils {
    private static Log _logger = LogFactory.getLog(AssetUtils.class);

    private static AssetUtils assetUtils;

    private AssetUtils(){}

    public static AssetUtils getInstance(){
        if(assetUtils != null){
            return assetUtils;
        }

        assetUtils = new AssetUtils();
        return assetUtils;
    }

    @LogEntryExitExecutionTime
    public Asset getAsset(AssetsRepository assetsRepository, String assetId) throws Exception {
        List<Asset> assetsById = assetsRepository.getAssetsById(assetId);
        if(!assetsById.isEmpty()){
            if(assetsById.size() == 1){
                return assetsById.get(0);
            }
            else{
                throw new IllegalArgumentException("There are multiple assets with ID '" + assetId + "'!");
            }
        }
        else{
            throw new IllegalArgumentException("There is no asset with ID '" + assetId + "'!");
        }
    }

    @LogEntryExitExecutionTime
    public boolean isSubscribed(AssetUserRepository assetUserRepository, User user, Asset asset){
        List<AssetUser> assetUsersByUserId = assetUserRepository.findAssetUsersByUserId(user.getId());
        for(AssetUser assetUser: assetUsersByUserId){
            if(assetUser.getAssetId().equalsIgnoreCase(asset.getId())){
                return true;
            }
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public int moveAssets(ContainerAssetsRepository containerAssetsRepository, AssetsRepository assetsRepository, List<String> assetIds, Container targetContainer) throws Exception {
        int numberOfIgnoredFiles = 0;

        for(String assetId: assetIds){
            boolean assetAlreadyInTargetContainer = false;
            Asset asset = AssetUtils.getInstance().getAsset(assetsRepository, assetId);

            Asset duplicateAsset = null;
            List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(targetContainer.getId());
            for(ContainerAsset containerAsset: containerAssetsByContainerId){
                Asset assetInContainer = AssetUtils.getInstance().getAsset(assetsRepository, containerAsset.getAssetId());
                if(assetInContainer.getId().equalsIgnoreCase(asset.getId())){
                    assetAlreadyInTargetContainer = true;
                    break;
                }
                if(assetInContainer.getName().equalsIgnoreCase(asset.getName()) && assetInContainer.getIsLatestVersion().equalsIgnoreCase("Y") && assetInContainer.getDeleted().equalsIgnoreCase("N")){
                    duplicateAsset = assetInContainer;
                    break;
                }
            }

            if(!assetAlreadyInTargetContainer){
                if(duplicateAsset != null){
                    // Container has a duplicate asset
                    // We must add the new asset and its versions as new versions of the duplicate asset.
                    List<Asset> duplicateAssetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(duplicateAsset.getOriginalAssetId());
                    assetsRepository.updateAssetsLatestVersion("N", duplicateAssetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));

                    List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                    containerAssetsRepository.moveAssets(targetContainer.getId(), assetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));

                    assetsRepository.updateAssetsOriginalAssetId(duplicateAsset.getOriginalAssetId(), assetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));

                    int latestVersionOfDuplicate = duplicateAsset.getVersion();

                    SortUtils.getInstance().sortItems("asc", assetsByOriginalAssetId, Comparator.comparing(Asset::getVersion));
                    for(Asset movedAsset : assetsByOriginalAssetId){
                        latestVersionOfDuplicate++;
                        assetsRepository.updateAssetVersion(latestVersionOfDuplicate, movedAsset.getId());
                    }
                }
                else{
                    // Container does not have duplicate assets
                    List<Asset> assetsByOriginalAssetId = assetsRepository.getNonDeletedAssetsByOriginalAssetId(asset.getOriginalAssetId());
                    containerAssetsRepository.moveAssets(targetContainer.getId(), assetsByOriginalAssetId.stream().map(Asset::getId).collect(Collectors.toList()));
                }
            }
            else{
                numberOfIgnoredFiles++;
                _logger.debug("Asset with ID '" + asset.getId() + " is already in the container with ID '" + targetContainer.getId() + "'. Skipping move...");
            }
        }

        return numberOfIgnoredFiles;
    }

    @LogEntryExitExecutionTime
    public int copyAssets(AssetsRepository assetsRepository, DiscoveryClient discoveryClient, HttpSession session,
                          String jobServiceLoadBalancerServiceName, List<String> sourceAssetIds, List<String> targetContainerIds, String username,
                          String importStagingPath) throws Exception {
        int numberOfFailedJobs = 0;

        for(String targetContainerId : targetContainerIds){
            numberOfFailedJobs += duplicateAssets(assetsRepository, discoveryClient, session, jobServiceLoadBalancerServiceName, sourceAssetIds, importStagingPath, targetContainerId, username);
        }

        return numberOfFailedJobs;
    }

    @LogEntryExitExecutionTime
    private int duplicateAssets(AssetsRepository assetsRepository, DiscoveryClient discoveryClient, HttpSession session, String jobServiceLoadBalancerServiceName, List<String> assetIds, String importStagingPath, String targetContainerId, String username) throws Exception {
        int numberOfFailedAssets = 0;

        UploadFileLst uploadFileLst = new UploadFileLst();
        uploadFileLst.setContainerId(targetContainerId);

        List<UploadFile> uploadFiles = new ArrayList<>();
        for(String assetId: assetIds){
            Asset asset = getAsset(assetsRepository, assetId);
            File currentFile = new File(asset.getPath());

            String tempFolderName = Long.toString(System.currentTimeMillis());
            String tempImportStagingPath = importStagingPath + File.separator + tempFolderName;
            _logger.debug("Import staging path: " + tempImportStagingPath);

            File tempFolder = new File(tempImportStagingPath);
            if(!tempFolder.exists()){
                if(!tempFolder.mkdir()){
                    throw new IOException("Unable to generate the temp folder path '" + tempImportStagingPath + "'!");
                }
            }

            String destinationPath;
            if(tempFolder.exists()){
                destinationPath = tempFolder.getAbsolutePath() + File.separator + asset.getName();
            }
            else{
                throw new FileNotFoundException("The temp folder '" + tempFolder.getAbsolutePath() + "' does not exist!");
            }

            Files.copy(currentFile.toPath(), new File(destinationPath).toPath());

            UploadFile uploadFile = new UploadFile();
            uploadFile.setUsername(username);
            uploadFile.setPath(destinationPath);

            uploadFiles.add(uploadFile);
        }

        uploadFileLst.setUploadFiles(uploadFiles);

        String jobServiceUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, jobServiceLoadBalancerServiceName);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);

        HttpEntity<UploadFileLst> uploadFileLstEntity = new HttpEntity<>(uploadFileLst, headers);
        ResponseEntity<JobResponse> jobResponseResponseEntity = restTemplate.postForEntity(jobServiceUrl + "/jobs/import", uploadFileLstEntity, JobResponse.class);
        boolean successful = jobResponseResponseEntity.getStatusCode().is2xxSuccessful();

        if(successful){
            _logger.debug("Asset import job successfully started!");
        }
        else{
            _logger.error("Asset import job failed to start. " + jobResponseResponseEntity.getBody().getMessage());
            numberOfFailedAssets += uploadFileLst.getUploadFiles().size();
        }

        return numberOfFailedAssets;
    }

    @LogEntryExitExecutionTime
    public Asset updateAsset(AssetsRepository assetsRepository, UpdateAssetRequest updateAssetRequest, String assetId) throws Exception {
        Asset asset = AssetUtils.getInstance().getAsset(assetsRepository, assetId);
        boolean updateChecksum = StringUtils.isNotBlank(updateAssetRequest.getChecksum());
        boolean updateDeleted = StringUtils.isNotBlank(updateAssetRequest.getDeleted());
        boolean updateExtension = StringUtils.isNotBlank(updateAssetRequest.getExtension());
        boolean updateFileSize = StringUtils.isNotBlank(updateAssetRequest.getFileSize());
        boolean updateImportDate = updateAssetRequest.getImportDate() != null;
        boolean updateImportedByUsername = StringUtils.isNotBlank(updateAssetRequest.getImportedByUsername());
        boolean updateIsLatestVersion = StringUtils.isNotBlank(updateAssetRequest.getIsLatestVersion());
        boolean updateName = StringUtils.isNotBlank(updateAssetRequest.getName());
        boolean updateOriginalAssetId = StringUtils.isNotBlank(updateAssetRequest.getOriginalAssetId());
        boolean updatePath = StringUtils.isNotBlank(updateAssetRequest.getPath());
        boolean updatePreviewPath = StringUtils.isNotBlank(updateAssetRequest.getPreviewPath());
        boolean updateThumbnailPath = StringUtils.isNotBlank(updateAssetRequest.getThumbnailPath());
        boolean updateType = StringUtils.isNotBlank(updateAssetRequest.getType());
        boolean updateVersion = updateAssetRequest.getVersion() != 0;

        asset.setChecksum(updateChecksum ? updateAssetRequest.getChecksum() : asset.getChecksum());
        asset.setDeleted(updateDeleted ? updateAssetRequest.getDeleted() : asset.getDeleted());
        asset.setExtension(updateExtension ? updateAssetRequest.getExtension() : asset.getExtension());
        asset.setFileSize(updateFileSize ? updateAssetRequest.getFileSize() : asset.getFileSize());
        asset.setImportDate(updateImportDate ? updateAssetRequest.getImportDate() : asset.getImportDate());
        asset.setImportedByUsername(updateImportedByUsername ? updateAssetRequest.getImportedByUsername() : asset.getImportedByUsername());
        asset.setIsLatestVersion(updateIsLatestVersion ? updateAssetRequest.getIsLatestVersion() : asset.getIsLatestVersion());
        asset.setName(updateName ? updateAssetRequest.getName() : asset.getName());
        asset.setOriginalAssetId(updateOriginalAssetId ? updateAssetRequest.getOriginalAssetId() : asset.getOriginalAssetId());
        asset.setPath(updatePath ? updateAssetRequest.getPath() : asset.getPath());
        asset.setPreviewPath(updatePreviewPath ? updateAssetRequest.getPreviewPath() : asset.getPreviewPath());
        asset.setThumbnailPath(updateThumbnailPath ? updateAssetRequest.getThumbnailPath() : asset.getThumbnailPath());
        asset.setType(updateType ? updateAssetRequest.getType() : asset.getType());
        asset.setVersion(updateVersion ? updateAssetRequest.getVersion() : asset.getVersion());
        assetsRepository.save(asset);

        if(updateName){
            List<Asset> assetsByOriginalAssetId = assetsRepository.getAssetsByOriginalAssetId(asset.getOriginalAssetId());
            if(!assetsByOriginalAssetId.isEmpty()){
                for(Asset versionedAsset: assetsByOriginalAssetId){
                    String extension = versionedAsset.getExtension().toLowerCase();
                    String newFileName = updateAssetRequest.getName() + "." + extension;
                    File oldFile = new File(versionedAsset.getPath());
                    if(oldFile.exists()){
                        String parentDirectoryPath = oldFile.getParentFile().getAbsolutePath();
                        String newFilePath = parentDirectoryPath + File.separator + newFileName;
                        File newFile = new File(newFilePath);
                        FileUtils.moveFile(oldFile, newFile);

                        assetsRepository.updateAssetName(newFileName, newFilePath, versionedAsset.getId());
                    }
                    else{
                        throw new IOException("File path " + versionedAsset.getPath() + " is not a valid file!");
                    }
                }
            }
            else{
                throw new IllegalArgumentException("There are no assets with the original asset ID '" + asset.getOriginalAssetId() + "'!");
            }
        }

        return asset;
    }
}