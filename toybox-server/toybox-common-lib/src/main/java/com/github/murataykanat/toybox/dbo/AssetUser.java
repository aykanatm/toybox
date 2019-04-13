package com.github.murataykanat.toybox.dbo;

import javax.persistence.*;

@Entity
@Table(name = "asset_user")
public class AssetUser {
    @Id
    @Column(name = "asset_id")
    private String assetId;
    @Column(name = "user_id")
    private int userId;

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
