package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDataType;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "assets")
public class Asset implements Serializable, ContainerItem {
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

    @Transient
    private String subscribed;

    @Transient
    private String shared;

    @Transient
    private String sharedByUsername;

    @Transient
    private String parentContainerId;

    @Column(name = "checksum")
    @JsonProperty("checksum")
    private String checksum;

    @Column(name = "is_latest_version")
    @JsonProperty("isLatestVersion")
    private String isLatestVersion;

    @Column(name = "original_asset_id")
    @JsonProperty("originalAssetId")
    private String originalAssetId;

    @Column(name = "version")
    @JsonProperty("version")
    private int version;

    @Column(name = "file_size")
    @JsonProperty("fileSize")
    private String fileSize;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = User.class)
    @JoinTable(name = "asset_user", joinColumns = @JoinColumn(name = "asset_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnore
    private Set<User> subscribers;

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

    public Set<User> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<User> subscribers) {
        this.subscribers = subscribers;
    }

    @Transient
    @JsonGetter(value = "subscribed")
    public String getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(String subscribed) {
        this.subscribed = subscribed;
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

    @Transient
    @JsonGetter(value = "parentContainerId")
    public String getParentContainerId() {
        return parentContainerId;
    }

    public void setParentContainerId(String parentContainerId) {
        this.parentContainerId = parentContainerId;
    }

    @Transient
    @JsonGetter(value = "shared")
    public String getShared() {
        return shared;
    }

    public void setShared(String shared) {
        this.shared = shared;
    }

    @Transient
    @JsonGetter(value = "sharedByUsername")
    public String getSharedByUsername() {
        return sharedByUsername;
    }

    public void setSharedByUsername(String sharedByUsername) {
        this.sharedByUsername = sharedByUsername;
    }
}
