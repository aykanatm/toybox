package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class MoveAssetRequest implements Serializable {
    @JsonProperty("assetIds")
    private List<String> assetIds;
    @JsonProperty("containerId")
    private String containerId;

    public List<String> getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(List<String> assetIds) {
        this.assetIds = assetIds;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
}
