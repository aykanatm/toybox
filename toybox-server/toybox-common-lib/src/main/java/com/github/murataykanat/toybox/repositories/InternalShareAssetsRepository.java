package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.InternalShareAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InternalShareAssetsRepository extends JpaRepository<InternalShareAsset, Integer> {
    @Query(value = "SELECT internal_share_id, asset_id FROM internal_share_assets WHERE internal_share_id=?1 AND asset_id=?2", nativeQuery = true)
    List<InternalShareAsset> findInternalShareAssetByInternalShareIdAndAssetId(String internalShareId, String assetId);

    @Query(value = "SELECT internal_share_id, asset_id FROM internal_share_assets WHERE asset_id=?1", nativeQuery = true)
    List<InternalShareAsset> findInternalShareAssetByAssetId(String assetId);

    @Query(value = "SELECT internal_share_id, asset_id FROM internal_share_assets WHERE internal_share_id=?1", nativeQuery = true)
    List<InternalShareAsset> findInternalShareAssetByInternalShareId(String internalShareId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO internal_share_assets(internal_share_id, asset_id) VALUES (?1, ?2)", nativeQuery = true)
    int insertSharedAsset(String internalShareId, String assetId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE internal_share_assets SET asset_id=?1 WHERE asset_id=?2", nativeQuery = true)
    int updateSharedAssets(String newAssetId, String oldAssetId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM internal_share_assets WHERE internal_share_id=?1", nativeQuery = true)
    int deleteSharedAssetByInternalShareId(String internalShareId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM internal_share_assets WHERE internal_share_id=?1 AND asset_id=?2", nativeQuery = true)
    int deleteSharedAssetByInternalShareIdAndAssetId(String internalShareId, String assetId);
}
