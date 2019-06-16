package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.ContainerAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ContainerAssetsRepository extends JpaRepository<ContainerAsset, String> {
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO container_asset(container_id, asset_id) VALUES (?1, ?2)", nativeQuery = true)
    int attachAsset(String containerId, String assetId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE container_asset SET container_id=:container_id WHERE asset_id IN :assetIds", nativeQuery = true)
    int moveAssets(@Param("container_id") String containerId, @Param("assetIds")List<String> assetIds);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM container_asset WHERE container_id=?1 AND asset_id=?2", nativeQuery = true)
    int deatchAsset(String containerId, String assetId);

    @Query(value = "SELECT container_id, asset_id FROM container_asset WHERE asset_id=?1", nativeQuery = true)
    List<ContainerAsset> findContainerAssetsByAssetId(String assetId);

    @Query(value = "SELECT container_id, asset_id FROM container_asset WHERE container_id=?1", nativeQuery = true)
    List<ContainerAsset> findContainerAssetsByContainerId(String containerId);
}
