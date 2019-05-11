package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Asset;

import java.io.Serializable;
import java.util.List;

public class AssetVersionResponse implements Serializable {
    @JsonProperty("assets")
    private List<Asset> assets;
    @JsonProperty("message")
    private String message;

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
