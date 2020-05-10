package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.QAsset;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.SingleValueBinding;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface AssetsRepository extends JpaRepository<Asset, String>, QueryDslPredicateExecutor<Asset>, QuerydslBinderCustomizer<QAsset> {
    @Override
    default public void customize(QuerydslBindings bindings, QAsset root) {
        bindings.bind(String.class)
                .first((SingleValueBinding<StringPath, String>) StringExpression::containsIgnoreCase);
    }

    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version, file_size FROM assets WHERE asset_id=?1", nativeQuery = true)
    List<Asset> getAssetsById(String assetId);

    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version, file_size FROM assets WHERE deleted='N' AND is_latest_version='Y' AND asset_id IN :assetIds", nativeQuery = true)
    List<Asset> getNonDeletedLastVersionAssetsByAssetIds(@Param("assetIds")List<String> assetIds);

    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version, file_size FROM assets WHERE asset_id IN :assetIds", nativeQuery = true)
    List<Asset> getAssetsByAssetIds(@Param("assetIds")List<String> assetIds);

    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version, file_size FROM assets WHERE original_asset_id=?1 AND deleted='N'", nativeQuery = true)
    List<Asset> getNonDeletedAssetsByOriginalAssetId(String originalAssetId);

    @Query(value = "SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type, deleted, checksum, is_latest_version, original_asset_id, version, file_size FROM assets WHERE original_asset_id=?1", nativeQuery = true)
    List<Asset> getAssetsByOriginalAssetId(String originalAssetId);

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
    @Query(value = "UPDATE assets SET version=?1 WHERE asset_id=?2", nativeQuery = true)
    int updateAssetVersion(int version, String assetId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE assets SET is_latest_version=:isLatestVersion WHERE asset_id IN :assetIds", nativeQuery = true)
    int updateAssetsLatestVersion(@Param("isLatestVersion") String isLatestVersion, @Param("assetIds")List<String> assetIds);

    @Transactional
    @Modifying
    @Query(value = "UPDATE assets SET original_asset_id=:originalAssetId WHERE asset_id IN :assetIds", nativeQuery = true)
    int updateAssetsOriginalAssetId(@Param("originalAssetId") String originalAssetId, @Param("assetIds")List<String> assetIds);

    @Modifying
    @Query(value = "INSERT INTO assets(asset_id, asset_extension, asset_imported_by_username, asset_name, asset_path, " +
            "asset_preview_path, asset_thumbnail_path, asset_type, asset_import_date, deleted, checksum, is_latest_version, original_asset_id, version, file_size) VALUES (:asset_id, :asset_extension, " +
            ":asset_imported_by_username, :asset_name, :asset_path, :asset_preview_path, :asset_thumbnail_path, :asset_type, :asset_import_date, :deleted, :checksum, :is_latest_version, :original_asset_id, :version, :file_size)", nativeQuery = true)
    int insertAsset(@Param("asset_id") String assetId, @Param("asset_extension") String extension, @Param("asset_imported_by_username") String username,
                    @Param("asset_name") String name, @Param("asset_path") String path, @Param("asset_preview_path") String previewPath,
                    @Param("asset_thumbnail_path") String thumbnailPath, @Param("asset_type") String mimeType, @Param("asset_import_date") Date importDate,
                    @Param("deleted") String deleted, @Param("checksum") String checksum, @Param("is_latest_version") String isLatestVersion,
                    @Param("original_asset_id") String originalAssetId, @Param("version") int version, @Param("file_size") String fileSize);
}
