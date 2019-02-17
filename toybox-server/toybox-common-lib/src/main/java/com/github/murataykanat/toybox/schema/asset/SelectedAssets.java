package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class SelectedAssets implements Serializable {
    @JsonProperty("selectedAssets")
    private List<String> selectedAssets;

    public List<String> getSelectedAssets() {
        return selectedAssets;
    }

    public void setSelectedAssets(List<String> selectedAssets) {
        this.selectedAssets = selectedAssets;
    }
}
