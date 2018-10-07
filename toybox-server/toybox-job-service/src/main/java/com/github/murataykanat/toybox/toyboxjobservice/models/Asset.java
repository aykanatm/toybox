package com.github.murataykanat.toybox.toyboxjobservice.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @Column(name = "asset_id")
    private String id;
    @Column(name = "asset_name")
    private String name;
    @Column(name = "asset_extension")
    private String extension;
    @Column(name = "asset_type")
    private String type;
    @Column(name = "asset_path")
    private String path;
    @Column(name = "asset_preview_path")
    private String previewPath;
    @Column(name = "asset_thumbnail_path")
    private String thumbnailPath;
    @Column(name = "asset_imported_by_username")
    private String importedByUsername;
    @Column(name = "asset_import_date")
    private String importDate;

    public Asset(){}

    public Asset(String assetId){
        this.id = assetId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getImportedByUsername() {
        return importedByUsername;
    }

    public void setImportedByUsername(String importedByUsername) {
        this.importedByUsername = importedByUsername;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }
}
