package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class CreateContainerRequest implements Serializable {
    @JsonProperty("parentContainerId")
    private String parentContainerId;
    @JsonProperty("containerName")
    private String containerName;

    public String getParentContainerId() {
        return parentContainerId;
    }

    public void setParentContainerId(String parentContainerId) {
        this.parentContainerId = parentContainerId;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }
}
