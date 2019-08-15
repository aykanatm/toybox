package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.AssetUser;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.AssetUserRepository;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

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
}
