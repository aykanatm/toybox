package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.models.UploadFile;
import com.github.murataykanat.toybox.models.UploadFileLst;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RefreshScope
@RestController
public class AssetController {
    private static final Log _logger = LogFactory.getLog(AssetController.class);

    @Value("${importStagingPath}")
    private String importStagingPath;

    // The name "upload" must match the "name" attribute of the input in UI (
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<UploadFileLst> uploadAssets(@RequestParam("upload") MultipartFile[] files) throws IOException {
        _logger.debug("uploadAssets() >>");
        _logger.debug("Import staging path: " + this.importStagingPath);

        try{
            UploadFileLst uploadFileLst = new UploadFileLst();
            List<UploadFile> uploadFiles = new ArrayList<>();

            for(MultipartFile file: files){
                String path = this.importStagingPath + File.separator + file.getOriginalFilename();
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

            // TODO: Rollback if any files are uploaded already

            UploadFileLst uploadFileLst = new UploadFileLst();
            uploadFileLst.setMessage(errorMessage);

            _logger.debug("<< uploadAssets()");
            return new ResponseEntity<>(uploadFileLst, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
