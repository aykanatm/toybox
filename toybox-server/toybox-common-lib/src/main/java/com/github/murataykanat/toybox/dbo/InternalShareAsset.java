package com.github.murataykanat.toybox.dbo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "internal_share_assets")
public class InternalShareAsset implements InternalShareItem{
    @Column(name = "internal_share_id")
    private String internalShareId;
    @Id
    @Column(name = "asset_id")
    private String assetId;

    public String getInternalShareId() {
        return internalShareId;
    }

    public void setInternalShareId(String internalShareId) {
        this.internalShareId = internalShareId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
}
