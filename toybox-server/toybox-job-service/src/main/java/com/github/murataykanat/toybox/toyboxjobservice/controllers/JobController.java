package com.github.murataykanat.toybox.toyboxjobservice.controllers;

import com.github.murataykanat.toybox.toyboxjobservice.models.UploadFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
public class JobController {
    private static final Log _logger = LogFactory.getLog(JobController.class);

    @RequestMapping(value = "/job/import", method = RequestMethod.POST)
    public void importAsset(@RequestBody ArrayList<UploadFile> uploadFiles){
        _logger.debug("importAsset() >>");
        _logger.debug("Uploaded files: ");
        for(UploadFile uploadFile: uploadFiles){
            _logger.debug(uploadFile.getPath());
        }
        _logger.debug("<< importAsset()");
    }
}
