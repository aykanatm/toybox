package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "internal_shares")
public class InternalShare {
    @Id
    @Column(name = "internal_share_id")
    @JsonProperty("internalShareId")
    private String internalShareId;

    @Column(name = "username")
    @JsonProperty("username")
    private String username;

    @Column(name = "enable_expire_internal")
    @JsonProperty("enableExpireInternal")
    private String enableExpireInternal;

    @Column(name = "expiration_date")
    @JsonProperty("expirationDate")
    private Date expirationDate;

    @Column(name = "notify_on_edit")
    @JsonProperty("notifyOnEdit")
    private String notifyOnEdit;

    @Column(name = "notify_on_download")
    @JsonProperty("notifyOnDownload")
    private String notifyOnDownload;

    @Column(name = "notify_on_share")
    @JsonProperty("notifyOnShare")
    private String notifyOnShare;

    @Column(name = "notify_on_copy")
    @JsonProperty("notifyOnCopy")
    private String notifyOnCopy;

    @Column(name = "can_edit")
    @JsonProperty("canEdit")
    private String canEdit;

    @Column(name = "can_download")
    @JsonProperty("canDownload")
    private String canDownload;

    @Column(name = "can_share")
    @JsonProperty("canShare")
    private String canShare;

    @Column(name = "can_copy")
    @JsonProperty("canCopy")
    private String canCopy;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "internal_share_assets", joinColumns = @JoinColumn(name = "internal_share_id"), inverseJoinColumns = @JoinColumn(name = "asset_id"))
    private Set<Asset> assets;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "internal_share_containers", joinColumns = @JoinColumn(name = "internal_share_id"), inverseJoinColumns = @JoinColumn(name = "container_id"))
    private Set<Container> containers;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "internal_share_users", joinColumns = @JoinColumn(name = "internal_share_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> users;

    public String getInternalShareId() {
        return internalShareId;
    }

    public void setInternalShareId(String internalShareId) {
        this.internalShareId = internalShareId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getNotifyOnEdit() {
        return notifyOnEdit;
    }

    public void setNotifyOnEdit(String notifyOnEdit) {
        this.notifyOnEdit = notifyOnEdit;
    }

    public String getNotifyOnDownload() {
        return notifyOnDownload;
    }

    public void setNotifyOnDownload(String notifyOnDownload) {
        this.notifyOnDownload = notifyOnDownload;
    }

    public String getNotifyOnShare() {
        return notifyOnShare;
    }

    public void setNotifyOnShare(String notifyOnShare) {
        this.notifyOnShare = notifyOnShare;
    }

    public String getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(String canEdit) {
        this.canEdit = canEdit;
    }

    public String getCanDownload() {
        return canDownload;
    }

    public void setCanDownload(String canDownload) {
        this.canDownload = canDownload;
    }

    public String getCanShare() {
        return canShare;
    }

    public void setCanShare(String canShare) {
        this.canShare = canShare;
    }

    public Set<Asset> getAssets() {
        return assets;
    }

    public void setAssets(Set<Asset> assets) {
        this.assets = assets;
    }

    public Set<Container> getContainers() {
        return containers;
    }

    public void setContainers(Set<Container> containers) {
        this.containers = containers;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public String getCanCopy() {
        return canCopy;
    }

    public void setCanCopy(String canCopy) {
        this.canCopy = canCopy;
    }

    public String getNotifyOnCopy() {
        return notifyOnCopy;
    }

    public void setNotifyOnCopy(String notifyOnCopy) {
        this.notifyOnCopy = notifyOnCopy;
    }

    public String getEnableExpireInternal() {
        return enableExpireInternal;
    }

    public void setEnableExpireInternal(String enableExpireInternal) {
        this.enableExpireInternal = enableExpireInternal;
    }
}