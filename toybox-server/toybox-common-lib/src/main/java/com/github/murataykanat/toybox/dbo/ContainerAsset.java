package com.github.murataykanat.toybox.dbo;

import javax.persistence.*;

@Entity
@Table(name = "container_asset")
public class ContainerAsset {
    @Column(name = "container_id")
    private String containerId;

    @Id
    @Column(name = "asset_id")
    private String assetId;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
}
