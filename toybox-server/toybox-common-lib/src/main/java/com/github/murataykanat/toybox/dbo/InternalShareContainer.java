package com.github.murataykanat.toybox.dbo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "internal_share_containers")
public class InternalShareContainer {
    @Id
    @Column(name = "internal_share_id")
    private String internalShareId;
    @Column(name = "container_id")
    private String containerId;

    public String getInternalShareId() {
        return internalShareId;
    }

    public void setInternalShareId(String internalShareId) {
        this.internalShareId = internalShareId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
}
