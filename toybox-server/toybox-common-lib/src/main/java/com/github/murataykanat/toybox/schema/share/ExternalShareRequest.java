package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class ExternalShareRequest implements Serializable {
    @JsonProperty("selectedAssets")
    private List<Asset> selectedAssets;
    @JsonProperty("selectedContainers")
    private List<Container> selectedContainers;
    @JsonProperty("expirationDate")
    private Date expirationDate;
    @JsonProperty("maxNumberOfHits")
    private int maxNumberOfHits;
    @JsonProperty("notifyWhenDownloaded")
    private boolean notifyWhenDownloaded;

    public List<Asset> getSelectedAssets() {
        return selectedAssets;
    }

    public void setSelectedAssets(List<Asset> selectedAssets) {
        this.selectedAssets = selectedAssets;
    }

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

    public List<Container> getSelectedContainers() {
        return selectedContainers;
    }

    public void setSelectedContainers(List<Container> selectedContainers) {
        this.selectedContainers = selectedContainers;
    }
}
