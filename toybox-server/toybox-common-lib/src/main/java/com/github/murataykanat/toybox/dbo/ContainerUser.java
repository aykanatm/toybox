package com.github.murataykanat.toybox.dbo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "container_user")
public class ContainerUser {
    @Id
    @Column(name = "container_id")
    private String containerId;
    @Column(name = "user_id")
    private int userId;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
