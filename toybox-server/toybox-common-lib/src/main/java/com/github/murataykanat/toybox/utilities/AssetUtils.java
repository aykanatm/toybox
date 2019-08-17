package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
                throw new Exception("There are multiple assets with ID '" + assetId + "'!");
            }
        }
        else{
            throw new Exception("There is no asset with ID '" + assetId + "'!");
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
}
