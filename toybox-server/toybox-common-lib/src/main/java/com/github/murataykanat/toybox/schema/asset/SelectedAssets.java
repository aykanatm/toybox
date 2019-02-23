package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Asset;

import java.io.Serializable;
import java.util.List;

public class SelectedAssets implements Serializable {
    @JsonProperty("selectedAssets")
    private List<Asset> selectedAssets;

    public List<Asset> getSelectedAssets() {
        return selectedAssets;
    }

    public void setSelectedAssets(List<Asset> selectedAssets) {
        this.selectedAssets = selectedAssets;
    }
}
