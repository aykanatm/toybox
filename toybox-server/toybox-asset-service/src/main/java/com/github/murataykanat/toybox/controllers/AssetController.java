package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import com.github.murataykanat.toybox.models.asset.ToyboxAsset;
import com.github.murataykanat.toybox.models.dbo.Asset;
import com.github.murataykanat.toybox.models.dbo.mappers.asset.AssetRowMapper;
import com.github.murataykanat.toybox.models.job.ToyboxJob;
import com.github.murataykanat.toybox.models.job.ToyboxJobFacet;
import com.github.murataykanat.toybox.schema.asset.AssetSearchRequest;
import com.github.murataykanat.toybox.schema.asset.RetrieveAssetsResults;
import com.github.murataykanat.toybox.schema.common.Facet;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.upload.UploadFile;
import com.github.murataykanat.toybox.schema.upload.UploadFileLst;
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

            List<Asset> allAssets = jdbcTemplate.query("SELECT asset_id, asset_import_date, asset_imported_by_username, asset_name, asset_path, asset_preview_path, asset_thumbnail_path, asset_type FROM assets", new AssetRowMapper());
            if(!allAssets.isEmpty()){
                List<ToyboxAsset> allToyboxAssets = allAssets.stream().map(asset -> new ToyboxAsset(asset)).collect(Collectors.toList());

                List<ToyboxAsset> assets;

                if(searchRequestFacetList != null && !searchRequestFacetList.isEmpty()){
                    assets = allToyboxAssets.stream().filter(toyboxAsset -> toyboxAsset.hasFacetValue(searchRequestFacetList)).collect(Collectors.toList());
                }
                else{
                    assets = allToyboxAssets;
                }

                List<String> facets = Arrays.asList(Asset.class.getDeclaredFields())
                        .stream()
                        .filter(f -> nonNull(f.getAnnotation(FacetColumnName.class)))
                        .map(f -> f.getAnnotation(FacetColumnName.class).value())
                        .collect(Collectors.toList());

                List<Facet> assetFacets = new ArrayList<>();

                // TODO: Convert this to stream implementation
                for(String facetName: facets){
                    Facet assetFacet = new Facet();
                    assetFacet.setName(facetName);

                    List<String> lookups = new ArrayList<>();

                    for(ToyboxAsset asset: assets ){
                        for(Method method: asset.getClass().getDeclaredMethods()){
                            if(method.getAnnotation(FacetColumnName.class) != null)
                            {
                                String facetColumnName = method.getAnnotation(FacetColumnName.class).value();
                                if(facetColumnName != null && facetColumnName.equalsIgnoreCase(facetName)){
                                    if(method.getAnnotation(FacetDefaultLookup.class) != null)
                                    {
                                        String[] defaultLookups = method.getAnnotation(FacetDefaultLookup.class).values();
                                        for(String defaultLookup: defaultLookups){
                                            lookups.add(defaultLookup);
                                        }
                                    }
                                    else
                                    {
                                        String lookup = (String) method.invoke(asset);
                                        lookups.add(lookup);
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    Set<String> uniqueLookupValues = new HashSet<>(lookups);
                    assetFacet.setLookups(new ArrayList<>(uniqueLookupValues));
                    assetFacets.add(assetFacet);
                }

                retrieveAssetsResults.setFacets(assetFacets);

                if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_import_date")){
                    sortAssets(sortType, assets, Comparator.comparing(ToyboxAsset::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
                }
                else if(StringUtils.isNotBlank(sortColumn) && sortColumn.equalsIgnoreCase("asset_name")){
                    sortAssets(sortType, assets, Comparator.comparing(ToyboxAsset::getName, Comparator.nullsLast(Comparator.naturalOrder())));
                }


                // TODO: If an admin users gets the jobs, display all jobs regardless of the username
                // List<Asset> assetsByCurrentUser = assets.stream().filter(a -> a.getImportedByUsername() != null && a.getImportedByUsername().equalsIgnoreCase(username)).collect(Collectors.toList());

                int totalRecords = assets.size();
                int startIndex = offset;
                int endIndex = (offset + limit) < totalRecords ? (offset + limit) : totalRecords;

                List<ToyboxAsset> assetsOnPage = assets.subList(startIndex, endIndex);


                retrieveAssetsResults.setTotalRecords(totalRecords);
                retrieveAssetsResults.setAssets(assetsOnPage);

                _logger.debug("<< retrieveJobs()");
                retrieveAssetsResults.setMessage("Jobs retrieved successfully!");
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

    private void sortAssets(String sortType, List<ToyboxAsset> allAssets, Comparator<ToyboxAsset> comparing) {
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
