package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public class UpdateContainerRequest implements Serializable {
    @JsonProperty("parentId")
    private String parentId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("createdByUsername")
    private String createdByUsername;
    @JsonProperty("creationDate")
    private Date creationDate;
    @JsonProperty("deleted")
    private String deleted;
    @JsonProperty("isSystem")
    private String isSystem;

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    public String getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(String isSystem) {
        this.isSystem = isSystem;
    }
}
