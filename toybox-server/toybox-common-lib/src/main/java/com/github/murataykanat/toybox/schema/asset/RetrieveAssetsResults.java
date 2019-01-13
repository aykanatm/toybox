package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.models.asset.ToyboxAsset;
import com.github.murataykanat.toybox.schema.common.Facet;

import java.io.Serializable;
import java.util.List;

public class RetrieveAssetsResults implements Serializable {
    @JsonProperty("assets")
    private List<ToyboxAsset> assets;
    @JsonProperty("totalRecords")
    private int totalRecords;
    @JsonProperty("facets")
    private List<Facet> facets;
    @JsonProperty("message")
    private String message;

    public List<ToyboxAsset> getAssets() {
        return assets;
    }

    public void setAssets(List<ToyboxAsset> assets) {
        this.assets = assets;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
