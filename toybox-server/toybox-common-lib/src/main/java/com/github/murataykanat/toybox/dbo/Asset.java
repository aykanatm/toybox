package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDataType;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "assets")
public class Asset implements Serializable {
    @Id
    @Column(name = "asset_id")
    @JsonProperty("id")
    private String id;

    @Column(name = "asset_name")
    @JsonProperty("name")
    private String name;

    @Column(name = "asset_extension")
    @JsonProperty("extension")
    private String extension;

    @Column(name = "asset_type")
    @JsonProperty("type")
    @FacetColumnName("Mime Type")
    private String type;

    @Column(name = "asset_path")
    @JsonIgnore
    private String path;

    @Column(name = "asset_preview_path")
    @JsonIgnore
    private String previewPath;

    @Column(name = "asset_thumbnail_path")
    @JsonIgnore
    private String thumbnailPath;

    @Column(name = "asset_imported_by_username")
    @JsonProperty("importedByUsername")
    @FacetColumnName("Username")
    private String importedByUsername;

    @Column(name = "asset_import_date")
    @JsonProperty("importDate")
    @FacetColumnName("Import Date")
    @FacetDataType(value = "Date")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date importDate;

    @Column(name = "deleted")
    @JsonProperty("deleted")
    private String deleted;

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

    public Date getImportDate() {
        return importDate;
    }

    public void setImportDate(Date importDate) {
        this.importDate = importDate;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
}
