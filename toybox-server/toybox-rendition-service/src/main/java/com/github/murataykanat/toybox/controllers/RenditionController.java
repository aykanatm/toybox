package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.mappers.asset.AssetRowMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.InvalidObjectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
public class RenditionController {
    private static final Log _logger = LogFactory.getLog(RenditionController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @RequestMapping(value = "/renditions/{assetId}/{renditionType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getRendition(@PathVariable String assetId, @PathVariable String renditionType){
        _logger.debug("getRendition() >>");
        try{
            Asset asset = getAsset(assetId);
            if(asset != null){
                ByteArrayResource resource;
                if(renditionType.equalsIgnoreCase("t")){
                    if(StringUtils.isNotBlank(asset.getThumbnailPath())){
                        Path path = Paths.get(asset.getThumbnailPath());
                        resource = new ByteArrayResource(Files.readAllBytes(path));
                    }
                    else{
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                }
                else if(renditionType.equalsIgnoreCase("p")){
                    if(StringUtils.isNotBlank(asset.getPreviewPath())){
                        Path path = Paths.get(asset.getPreviewPath());
                        resource = new ByteArrayResource(Files.readAllBytes(path));
                    }
                    else{
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                }
                else{
                    throw new IllegalArgumentException("Rendition type is not recognized!");
                }

                return new ResponseEntity<>(resource, HttpStatus.OK);
            }
            else{
                throw new InvalidObjectException("Asset is null!");
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the asset with ID " + assetId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< getRendition()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Asset getAsset(String assetId){
        _logger.debug("getAsset() >> [" + assetId + "]");
        Asset result = null;

        List<Asset> assets = jdbcTemplate.query("SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type FROM assets WHERE asset_id=?", new Object[]{assetId},  new AssetRowMapper());
        if(assets != null){
            if(!assets.isEmpty()){
                if(assets.size() == 1){
                    result = assets.get(0);
                }
                else{
                    throw new DuplicateKeyException("Asset ID " + assetId + " is duplicate!");
                }
            }
        }

        _logger.debug("<< getAsset()");
        return result;
    }
}