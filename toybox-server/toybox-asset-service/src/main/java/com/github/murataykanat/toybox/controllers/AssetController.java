package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.mappers.asset.AssetRowMapper;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.asset.RetrieveAssetsResults;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import com.github.murataykanat.toybox.utilities.SortUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@RestController
public class AssetController {
    private static final Log _logger = LogFactory.getLog(AssetController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${importStagingPath}")
    private String importStagingPath;

    @RequestMapping(value = "/assets/{assetId}/download", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadAsset(@PathVariable String assetId){
        _logger.debug("downloadAsset() >> [" + assetId + "]");
        try{
            Asset asset = getAsset(assetId);
            if(asset != null){
                if(StringUtils.isNotBlank(asset.getPath())){
                    File file = new File(asset.getPath());
                    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

                    _logger.debug("<< downloadAsset()");
                    return new ResponseEntity<>(resource, HttpStatus.OK);

                }
                else{
                    _logger.error("Asset path is blank!");
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            }
            else{
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while the asset with ID " + assetId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< downloadAsset()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // The name "upload" must match the "name" attribute of the input in UI (
    @RequestMapping(value = "/assets/upload", method = RequestMethod.POST)
    public ResponseEntity<UploadFileLst> uploadAssets(Authentication authentication, @RequestParam("upload") MultipartFile[] files) {
        _logger.debug("uploadAssets() >>");

        String tempFolderName = Long.toString(System.currentTimeMillis());
        String tempImportStagingPath = importStagingPath + File.separator + tempFolderName;
        _logger.debug("Import staging path: " + tempImportStagingPath);

        try{
            File tempFolder = new File(tempImportStagingPath);
            if(!tempFolder.exists()){
                tempFolder.mkdir();
            }

            UploadFileLst uploadFileLst = new UploadFileLst();
            List<UploadFile> uploadFiles = new ArrayList<>();

            for(MultipartFile file: files){
                String path;
                if(tempFolder.exists()){
                    path = tempFolder.getAbsolutePath() + File.separator + file.getOriginalFilename();
                }
                else{
                    throw new FileNotFoundException("The temp folder " + tempFolder.getAbsolutePath() + " does not exist!");
                }

                file.transferTo(new File(path));

                UploadFile uploadFile = new UploadFile();
                uploadFile.setPath(path);
                uploadFile.setUsername(authentication.getName());

                uploadFiles.add(uploadFile);
            }

            uploadFileLst.setUploadFiles(uploadFiles);
            uploadFileLst.setMessage("Files uploaded successfully!");

            _logger.debug("<< uploadAssets()");
            return new ResponseEntity<>(uploadFileLst, HttpStatus.CREATED);
        }
        catch (Exception e){
            String errorMessage = "An error occurred while uploading files. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            File tempFolder = new File(tempImportStagingPath);
            try {
                if(tempFolder.exists()){
                    FileUtils.deleteDirectory(tempFolder);
                }
            }
            catch (IOException ioe){
                String ioErrorMessage = ". An error occurred while deleting the temp files. " + ioe.getLocalizedMessage();
                errorMessage += ioErrorMessage;
            }

            UploadFileLst uploadFileLst = new UploadFileLst();
            uploadFileLst.setMessage(errorMessage);

            _logger.debug("<< uploadAssets()");
            return new ResponseEntity<>(uploadFileLst, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/assets/search", method = RequestMethod.POST)
    public ResponseEntity<RetrieveAssetsResults> retrieveAssets(Authentication authentication, @RequestBody AssetSearchRequest assetSearchRequest){
        _logger.debug("retrieveAssets() >>");

        try{
            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();

            String sortColumn = assetSearchRequest.getSortColumn();
            String sortType = assetSearchRequest.getSortType();
            int offset = assetSearchRequest.getOffset();
            int limit = assetSearchRequest.getLimit();
            List<SearchRequestFacet> searchRequestFacetList = assetSearchRequest.getAssetSearchRequestFacetList();

            List<Asset> allAssets = jdbcTemplate.query("SELECT asset_id, asset_extension, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type FROM assets", new AssetRowMapper());
            if(!allAssets.isEmpty()){

                List<Asset> assets;

                if(searchRequestFacetList != null && !searchRequestFacetList.isEmpty()){
                    assets = allAssets.stream().filter(asset -> FacetUtils.getInstance().hasFacetValue(asset, searchRequestFacetList)).collect(Collectors.toList());
                }
                else{
                    assets = allAssets;
                }

                List<Facet> facets = FacetUtils.getInstance().getFacets(assets);

                retrieveAssetsResults.setFacets(facets);

                if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
                    SortUtils.getInstance().sortItems(sortType, assets, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                }
                else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                    SortUtils.getInstance().sortItems(sortType, assets, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                }

                String username = authentication.getName();
                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

                List<Asset> assetsByCurrentUser;
                if(authorities.contains("ROLE_ADMIN")){
                    _logger.debug("Retrieving all assets [Admin User]...");
                    assetsByCurrentUser = assets;
                }
                else{
                    _logger.debug("Retrieving assets of the user '" + username + "'...");
                    assetsByCurrentUser = assets.stream().filter(a -> a.getImportedByUsername() != null && a.getImportedByUsername().equalsIgnoreCase(username)).collect(Collectors.toList());
                }

                int totalRecords = assets.size();
                int startIndex = offset;
                int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                List<Asset> assetsOnPage = assetsByCurrentUser.subList(startIndex, endIndex);


                retrieveAssetsResults.setTotalRecords(totalRecords);
                retrieveAssetsResults.setAssets(assetsOnPage);

                _logger.debug("<< retrieveAssets()");
                retrieveAssetsResults.setMessage("Assets retrieved successfully!");
                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.OK);
            }
            else{
                String message = "There is no assets to return.";
                _logger.debug(message);

                retrieveAssetsResults.setMessage(message);

                _logger.debug("<< retrieveJobs()");
                return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.NO_CONTENT);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving assets. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            RetrieveAssetsResults retrieveAssetsResults = new RetrieveAssetsResults();
            retrieveAssetsResults.setMessage(errorMessage);

            _logger.debug("<< retrieveAssets()");
            return new ResponseEntity<>(retrieveAssetsResults, HttpStatus.INTERNAL_SERVER_ERROR);
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
