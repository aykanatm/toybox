package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class InternalShareRequest implements Serializable {
    @JsonProperty("selectionContext")
    private SelectionContext selectionContext;
    @JsonProperty("sharedUsergroups")
    private List<String> sharedUsergroups;
    @JsonProperty("sharedUsers")
    private List<String> sharedUsers;
    @JsonProperty("enableExpireInternal")
    private boolean enableExpireInternal;
    @JsonFormat(pattern="MM/dd/yyyy")
    @JsonProperty("expirationDate")
    private Date expirationDate;
    @JsonProperty("notifyOnEdit")
    private boolean notifyOnEdit;
    @JsonProperty("notifyOnDownload")
    private boolean notifyOnDownload;
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

    public List<String> getSharedUsergroups() {
        return sharedUsergroups;
    }

    public void setSharedUsergroups(List<String> sharedUsergroups) {
        this.sharedUsergroups = sharedUsergroups;
    }

    public boolean getEnableExpireInternal() {
        return enableExpireInternal;
    }

    public void setEnableExpireInternal(boolean enableExpireInternal) {
        this.enableExpireInternal = enableExpireInternal;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean getNotifyOnEdit() {
        return notifyOnEdit;
    }

    public void setNotifyOnEdit(boolean notifyOnEdit) {
        this.notifyOnEdit = notifyOnEdit;
    }

    public boolean getNotifyOnDownload() {
        return notifyOnDownload;
    }

    public void setNotifyOnDownload(boolean notifyOnDownload) {
        this.notifyOnDownload = notifyOnDownload;
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

    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    public void setSelectionContext(SelectionContext selectionContext) {
        this.selectionContext = selectionContext;
    }

    public List<String> getSharedUsers() {
        return sharedUsers;
    }

    public void setSharedUsers(List<String> sharedUsers) {
        this.sharedUsers = sharedUsers;
    }

    public boolean getCanCopy() {
        return canCopy;
    }

    public void setCanCopy(boolean canCopy) {
        this.canCopy = canCopy;
    }
}