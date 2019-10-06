package com.github.murataykanat.toybox.dbo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "internal_share_users")
public class InternalShareUser {
    @Id
    @Column(name = "internal_share_id")
    private String internalShareId;
    @Column(name = "user_id")
    private int userId;

    public String getInternalShareId() {
        return internalShareId;
    }

    public void setInternalShareId(String internalShareId) {
        this.internalShareId = internalShareId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
