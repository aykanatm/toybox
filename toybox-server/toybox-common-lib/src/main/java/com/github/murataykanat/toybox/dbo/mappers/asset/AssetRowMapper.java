package com.github.murataykanat.toybox.dbo.mappers.asset;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.utilities.DateTimeUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AssetRowMapper implements RowMapper<Asset> {
    @Override
    public Asset mapRow(ResultSet resultSet, int i) throws SQLException {
        Asset asset = new Asset();
        asset.setId(resultSet.getString("asset_id"));
        asset.setExtension(resultSet.getString("asset_extension"));
        asset.setImportDate(DateTimeUtils.stringToDate(resultSet.getString("asset_import_date")));
        asset.setImportedByUsername(resultSet.getString("asset_imported_by_username"));
        asset.setName(resultSet.getString("asset_name"));
        asset.setPath(resultSet.getString("asset_path"));
        asset.setPreviewPath(resultSet.getString("asset_preview_path"));
        asset.setThumbnailPath(resultSet.getString("asset_thumbnail_path"));
        asset.setType(resultSet.getString("asset_type"));

        return asset;
    }
}
