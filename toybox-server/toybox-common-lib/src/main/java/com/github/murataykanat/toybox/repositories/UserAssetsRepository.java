package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.UserAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserAssetsRepository extends JpaRepository<UserAsset, Integer> {
    @Query(value = "SELECT asset_id, user_id FROM user_asset WHERE user_id=?1 AND asset_id=?2", nativeQuery = true)
    List<UserAsset> findUserAssetsByUserIdAndAssetId(int userId, String assetId);
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO user_asset(asset_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
    int insertSharedAsset(String assetId, int userId);
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM user_asset WHERE asset_id=?1 AND user_id=?2", nativeQuery = true)
    int deleteSharedAsset(String assetId, int userId);
}
