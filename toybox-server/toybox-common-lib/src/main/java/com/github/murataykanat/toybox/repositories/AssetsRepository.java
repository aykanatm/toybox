package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface AssetsRepository extends JpaRepository<Asset, String> {
    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version FROM assets WHERE deleted='N'", nativeQuery = true)
    List<Asset> getNonDeletedAssets();

    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version FROM assets WHERE asset_id=?1", nativeQuery = true)
    List<Asset> getAssetsById(String assetId);

    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version FROM assets WHERE asset_name=?1 AND asset_imported_by_username=?2", nativeQuery = true)
    List<Asset> getDuplicateAssetsByAssetNameAndUsername(String assetName, String username);

    @Modifying
    @Query(value = "UPDATE assets SET asset_thumbnail_path=?1 WHERE asset_id=?2", nativeQuery = true)
    int updateAssetThumbnailPath(String thumbnailPath, String assetId);

    @Modifying
    @Query(value = "UPDATE assets SET asset_preview_path=?1 WHERE asset_id=?2", nativeQuery = true)
    int updateAssetPreviewPath(String previewPath, String assetId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE assets SET asset_name=?1, asset_path=?2 WHERE asset_id=?3", nativeQuery = true)
    int updateAssetName(String assetName, String assetPath, String assetId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE assets SET is_latest_version=:isLatestVersion WHERE asset_id IN :assetIds", nativeQuery = true)
    int updateAssetsLatestVersion(@Param("isLatestVersion") String isLatestVersion, @Param("assetIds")List<String> assetIds);

    @Modifying
    @Query(value = "INSERT INTO assets(asset_id, asset_extension, asset_imported_by_username, asset_name, asset_path, " +
            "asset_preview_path, asset_thumbnail_path, asset_type, asset_import_date, deleted, checksum, is_latest_version, original_asset_id, version) VALUES (:asset_id, :asset_extension, " +
            ":asset_imported_by_username, :asset_name, :asset_path, :asset_preview_path, :asset_thumbnail_path, :asset_type, :asset_import_date, :deleted, :checksum, :is_latest_version, :original_asset_id, :version)", nativeQuery = true)
    int insertAsset(@Param("asset_id") String assetId, @Param("asset_extension") String extension, @Param("asset_imported_by_username") String username,
                    @Param("asset_name") String name, @Param("asset_path") String path, @Param("asset_preview_path") String previewPath,
                    @Param("asset_thumbnail_path") String thumbnailPath, @Param("asset_type") String mimeType, @Param("asset_import_date") Date importDate,
                    @Param("deleted") String deleted, @Param("checksum") String checksum, @Param("is_latest_version") String isLatestVersion,
                    @Param("original_asset_id") String originalAssetId, @Param("version") int version);

    @Transactional
    @Modifying
    @Query(value = "UPDATE assets SET deleted=?1 WHERE asset_id=?2", nativeQuery = true)
    int deleteAssetById(String deleted, String assetId);
}
