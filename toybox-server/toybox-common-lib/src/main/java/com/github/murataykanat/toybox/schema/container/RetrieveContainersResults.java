package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Container;

import java.io.Serializable;
import java.util.List;

public class RetrieveContainersResults implements Serializable {
    @JsonProperty("containers")
    private List<Container> containers;
    @JsonProperty("containerId")
    private String containerId;
    @JsonProperty("totalRecords")
    private int totalRecords;
    @JsonProperty("message")
    private String message;

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }
}
