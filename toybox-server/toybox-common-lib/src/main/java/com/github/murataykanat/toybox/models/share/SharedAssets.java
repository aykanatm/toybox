package com.github.murataykanat.toybox.models.share;

import java.util.List;

public class SharedAssets {
    private String username;
    private List<String> assetIds;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(List<String> assetIds) {
        this.assetIds = assetIds;
    }
}
