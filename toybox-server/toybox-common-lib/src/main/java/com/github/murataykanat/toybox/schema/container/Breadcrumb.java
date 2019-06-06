package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Breadcrumb implements Serializable {
    @JsonProperty("containerName")
    private String containerName;
    @JsonProperty("containerId")
    private String containerId;

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
}
