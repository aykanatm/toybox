package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.mappers.asset.AssetRowMapper;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.asset.RetrieveAssetsResults;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
import com.github.murataykanat.toybox.utilities.FacetUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@RefreshScope
@RestController
public class AssetController {
    private static final Log _logger = LogFactory.getLog(AssetController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${importStagingPath}")
    private String importStagingPath;

    // TODO: Make this /assets/upload
    // The name "upload" must match the "name" attribute of the input in UI (
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<UploadFileLst> uploadAssets(@RequestParam("upload") MultipartFile[] files) {
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
                // TODO: Take this value from the session
                uploadFile.setUsername("test");

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
    public ResponseEntity<RetrieveAssetsResults> retrieveAssets(@RequestBody AssetSearchRequest assetSearchRequest){
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
                    sortAssets(sortType, assets, Comparator.comparing(Asset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                }
                else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                    sortAssets(sortType, assets, Comparator.comparing(Asset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                }


                // TODO: If an admin users gets the jobs, display all jobs regardless of the username
                // List<Asset> assetsByCurrentUser = assets.stream().filter(a -> a.getImportedByUsername() != null && a.getImportedByUsername().equalsIgnoreCase(username)).collect(Collectors.toList());

                int totalRecords = assets.size();
                int startIndex = offset;
                int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                List<Asset> assetsOnPage = assets.subList(startIndex, endIndex);


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

    private void sortAssets(String sortType, List<Asset> allAssets, Comparator<Asset> comparing) {
        if(sortType.equalsIgnoreCase("des")){
            allAssets.sort(comparing.reversed());
        }
        else if(sortType.equalsIgnoreCase("asc")){
            allAssets.sort(comparing);
        }
        else{
            allAssets.sort(comparing.reversed());
        }
    }
}
