package com.github.murataykanat.toybox.schema.selection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.Container;

import java.io.Serializable;
import java.util.List;

public class SelectionContext implements Serializable {
    @JsonProperty("selectedAssets")
    private List<Asset> selectedAssets;
    @JsonProperty("selectedContainers")
    private List<Container> selectedContainers;

    public List<Asset> getSelectedAssets() {
        return selectedAssets;
    }

    public void setSelectedAssets(List<Asset> selectedAssets) {
        this.selectedAssets = selectedAssets;
    }

    public List<Container> getSelectedContainers() {
        return selectedContainers;
    }

    public void setSelectedContainers(List<Container> selectedContainers) {
        this.selectedContainers = selectedContainers;
    }
}
