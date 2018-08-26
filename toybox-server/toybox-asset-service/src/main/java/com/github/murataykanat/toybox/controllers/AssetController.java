package com.github.murataykanat.toybox.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AssetController {
    // The name "upload" must match the "name" attribute of the input in UI
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public void importAssets(@RequestParam("upload") MultipartFile[] files){
        for(MultipartFile file: files){
            // TODO:
            // Use config server or environment variable for staging location
            System.out.println(file.getOriginalFilename());
        }
    }
}
