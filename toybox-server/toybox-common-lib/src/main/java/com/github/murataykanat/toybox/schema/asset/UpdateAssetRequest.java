package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public class UpdateAssetRequest implements Serializable {
    @JsonProperty("name")
    private String name;
    @JsonProperty("extension")
    private String extension;
    @JsonProperty("type")
    private String type;
    @JsonProperty("path")
    private String path;
    @JsonProperty("previewPath")
    private String previewPath;
    @JsonProperty("thumbnailPath")
    private String thumbnailPath;
    @JsonProperty("importedByUsername")
    private String importedByUsername;
    @JsonProperty("importDate")
    private Date importDate;
    @JsonProperty("deleted")
    private String deleted;
    @JsonProperty("checksum")
    private String checksum;
    @JsonProperty("isLatestVersion")
    private String isLatestVersion;
    @JsonProperty("originalAssetId")
    private String originalAssetId;
    @JsonProperty("version")
    private int version;
    @JsonProperty("fileSize")
    private String fileSize;

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

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getIsLatestVersion() {
        return isLatestVersion;
    }

    public void setIsLatestVersion(String isLatestVersion) {
        this.isLatestVersion = isLatestVersion;
    }

    public String getOriginalAssetId() {
        return originalAssetId;
    }

    public void setOriginalAssetId(String originalAssetId) {
        this.originalAssetId = originalAssetId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }
}
