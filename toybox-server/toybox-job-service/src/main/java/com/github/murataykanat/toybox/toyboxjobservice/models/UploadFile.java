package com.github.murataykanat.toybox.toyboxjobservice.models;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;

public class UploadFile {
    @JsonProperty("path")
    private String path;
    @JsonProperty("username")
    private String username;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
