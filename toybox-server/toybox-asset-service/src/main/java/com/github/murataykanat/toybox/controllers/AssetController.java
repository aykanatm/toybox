package com.github.murataykanat.toybox.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RefreshScope
@RestController
public class AssetController {
    private static final Log _logger = LogFactory.getLog(AssetController.class);

    @Value("${importStagingPath}")
    private String importStagingPath;

    // The name "upload" must match the "name" attribute of the input in UI (
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public void uploadAssets(@RequestParam("upload") MultipartFile[] files){
        _logger.debug("uploadAssets() >>");
        _logger.debug("Import staging path: " + importStagingPath);

        try{
            for(MultipartFile file: files){
                file.transferTo(new File(importStagingPath + File.separator + file.getOriginalFilename()));
            }
            // TODO:
            // Start the import job
        }
        catch (Exception e){
            String errorMessage = "An error occured while uploading files. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
        }
        _logger.debug("<< uploadAssets()");
    }
}
