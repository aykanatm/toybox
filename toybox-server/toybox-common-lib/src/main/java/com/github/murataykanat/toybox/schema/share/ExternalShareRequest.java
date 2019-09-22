package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;

import java.io.Serializable;
import java.util.Date;

public class ExternalShareRequest implements Serializable {
    @JsonProperty("selectionContext")
    private SelectionContext selectionContext;
    @JsonProperty("enableExpireExternal")
    private boolean enableExpireExternal;
    @JsonFormat(pattern="MM/dd/yyyy")
    @JsonProperty("expirationDate")
    private Date expirationDate;
    @JsonProperty("enableUsageLimit")
    private boolean enableUsageLimit;
    @JsonProperty("maxNumberOfHits")
    private int maxNumberOfHits;
    @JsonProperty("notifyWhenDownloaded")
    private boolean notifyWhenDownloaded;

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public int getMaxNumberOfHits() {
        return maxNumberOfHits;
    }

    public void setMaxNumberOfHits(int maxNumberOfHits) {
        this.maxNumberOfHits = maxNumberOfHits;
    }

    public boolean getNotifyWhenDownloaded() {
        return notifyWhenDownloaded;
    }

    public void setNotifyWhenDownloaded(boolean notifyWhenDownloaded) {
        this.notifyWhenDownloaded = notifyWhenDownloaded;
    }

    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    public void setSelectionContext(SelectionContext selectionContext) {
        this.selectionContext = selectionContext;
    }

    public boolean getEnableExpireExternal() {
        return enableExpireExternal;
    }

    public void setEnableExpireExternal(boolean enableExpireExternal) {
        this.enableExpireExternal = enableExpireExternal;
    }

    public boolean getEnableUsageLimit() {
        return enableUsageLimit;
    }

    public void setEnableUsageLimit(boolean enableUsageLimit) {
        this.enableUsageLimit = enableUsageLimit;
    }
}
