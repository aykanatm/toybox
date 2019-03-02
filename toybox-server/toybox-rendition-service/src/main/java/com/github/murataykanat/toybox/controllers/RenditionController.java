package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InvalidObjectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
public class RenditionController {
    private static final Log _logger = LogFactory.getLog(RenditionController.class);

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private UsersRepository usersRepository;

    @RequestMapping(value = "/renditions/users/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getUserAvatar(@PathVariable String username){
        _logger.debug("getUserAvatar() >>");
        try{
            List<User> users = usersRepository.findUsersByUsername(username);
            if(users != null){
                if(users.size() == 1){
                    User user = users.get(0);
                    if(user != null){
                        ByteArrayResource resource;
                        if(StringUtils.isNotBlank(user.getAvatarPath())){
                            Path path = Paths.get(user.getAvatarPath());
                            resource = new ByteArrayResource(Files.readAllBytes(path));
                        }
                        else{
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }

                        return new ResponseEntity<>(resource, HttpStatus.OK);
                    }
                    else{
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                }
                else{
                    throw new DuplicateKeyException("There are more than one asset with ID '" + username + "'");
                }
            }
            else{
                _logger.warn("No user was found with username '" + username + "'");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the user with username " + username + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< getRendition()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/renditions/assets/{assetId}/{renditionType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getRendition(@PathVariable String assetId, @PathVariable String renditionType){
        _logger.debug("getRendition() >>");
        try{
            List<Asset> assets = assetsRepository.getAssetsById(assetId);
            if(assets != null){
                if(assets.size() == 1){
                    Asset asset = assets.get(0);
                    if(asset != null){
                        ByteArrayResource resource;
                        if(renditionType.equalsIgnoreCase("t")){
                            if(StringUtils.isNotBlank(asset.getThumbnailPath())){
                                Path path = Paths.get(asset.getThumbnailPath());
                                resource = new ByteArrayResource(Files.readAllBytes(path));

                                return new ResponseEntity<>(resource, HttpStatus.OK);
                            }
                            else{
                                _logger.error("Thumbnail path is blank!");
                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                            }
                        }
                        else if(renditionType.equalsIgnoreCase("p")){
                            if(StringUtils.isNotBlank(asset.getPreviewPath())){
                                Path path = Paths.get(asset.getPreviewPath());
                                resource = new ByteArrayResource(Files.readAllBytes(path));

                                if(asset.getPreviewPath().endsWith("pdf")){
                                    return ResponseEntity.ok().header("Content-Disposition","inline; filename=" + new File(asset.getPreviewPath()).getName()).contentType(MediaType.APPLICATION_PDF).contentLength(resource.contentLength()).body(resource);
                                }
                                else{
                                    return new ResponseEntity<>(resource, HttpStatus.OK);
                                }
                            }
                            else{
                                _logger.error("Preview path is blank!");
                                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                            }
                        }
                        else{
                            throw new IllegalArgumentException("Rendition type is not recognized!");
                        }
                    }
                    else{
                        throw new InvalidObjectException("Asset is null!");
                    }
                }
                else{
                    throw new DuplicateKeyException("There are more than one asset with ID '" + assetId + "'");
                }
            }
            else{
                _logger.warn("No asset was found with ID '" + assetId + "'");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the asset with ID " + assetId + ". " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            _logger.debug("<< getRendition()");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
