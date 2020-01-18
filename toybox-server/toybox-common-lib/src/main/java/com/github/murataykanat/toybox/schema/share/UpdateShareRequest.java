package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class UpdateShareRequest implements Serializable {
    @JsonProperty("enableExpire")
    private boolean enableExpire;
    @JsonFormat(pattern="MM/dd/yyyy")
    @JsonProperty("expirationDate")
    private Date expirationDate;
    @JsonProperty("notifyOnDownload")
    private boolean notifyOnDownload;
    @JsonProperty("type")
    private String type;

    @JsonProperty("notifyOnEdit")
    private boolean notifyOnEdit;
    @JsonProperty("notifyOnShare")
    private boolean notifyOnShare;
    @JsonProperty("notifyOnCopy")
    private boolean notifyOnCopy;
    @JsonProperty("canEdit")
    private boolean canEdit;
    @JsonProperty("canDownload")
    private boolean canDownload;
    @JsonProperty("canShare")
    private boolean canShare;
    @JsonProperty("canCopy")
    private boolean canCopy;
    @JsonProperty("sharedUsergroups")
    private List<String> sharedUsergroups;
    @JsonProperty("sharedUsers")
    private List<String> sharedUsers;

    @JsonProperty("enableUsageLimit")
    private boolean enableUsageLimit;
    @JsonProperty("maxNumberOfHits")
    private int maxNumberOfHits;

    public boolean getEnableExpire() {
        return enableExpire;
    }

    public void setEnableExpire(boolean enableExpire) {
        this.enableExpire = enableExpire;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean getNotifyOnDownload() {
        return notifyOnDownload;
    }

    public void setNotifyOnDownload(boolean notifyOnDownload) {
        this.notifyOnDownload = notifyOnDownload;
    }

    public boolean getNotifyOnEdit() {
        return notifyOnEdit;
    }

    public void setNotifyOnEdit(boolean notifyOnEdit) {
        this.notifyOnEdit = notifyOnEdit;
    }

    public boolean getNotifyOnShare() {
        return notifyOnShare;
    }

    public void setNotifyOnShare(boolean notifyOnShare) {
        this.notifyOnShare = notifyOnShare;
    }

    public boolean getNotifyOnCopy() {
        return notifyOnCopy;
    }

    public void setNotifyOnCopy(boolean notifyOnCopy) {
        this.notifyOnCopy = notifyOnCopy;
    }

    public boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean getCanDownload() {
        return canDownload;
    }

    public void setCanDownload(boolean canDownload) {
        this.canDownload = canDownload;
    }

    public boolean getCanShare() {
        return canShare;
    }

    public void setCanShare(boolean canShare) {
        this.canShare = canShare;
    }

    public boolean getCanCopy() {
        return canCopy;
    }

    public void setCanCopy(boolean canCopy) {
        this.canCopy = canCopy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getEnableUsageLimit() {
        return enableUsageLimit;
    }

    public void setEnableUsageLimit(boolean enableUsageLimit) {
        this.enableUsageLimit = enableUsageLimit;
    }

    public int getMaxNumberOfHits() {
        return maxNumberOfHits;
    }

    public void setMaxNumberOfHits(int maxNumberOfHits) {
        this.maxNumberOfHits = maxNumberOfHits;
    }

    public List<String> getSharedUsergroups() {
        return sharedUsergroups;
    }

    public void setSharedUsergroups(List<String> sharedUsergroups) {
        this.sharedUsergroups = sharedUsergroups;
    }

    public List<String> getSharedUsers() {
        return sharedUsers;
    }

    public void setSharedUsers(List<String> sharedUsers) {
        this.sharedUsers = sharedUsers;
    }
}