package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class CreateContainerResponse implements Serializable {
    @JsonProperty("message")
    private String message;
    @JsonProperty("containerId")
    private String containerId;

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
