package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class ExternalShareRequest implements Serializable {
    @JsonProperty("selectionContext")
    private SelectionContext selectionContext;
    @JsonProperty("expirationDate")
    private Date expirationDate;
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
}
