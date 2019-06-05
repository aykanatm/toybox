package com.github.murataykanat.toybox.schema.upload;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class UploadFileLst implements Serializable {
    @JsonProperty("uploadFiles")
    private List<UploadFile> uploadFiles;
    @JsonProperty("containerId")
    private String containerId;
    @JsonProperty("message")
    private String message;

    public List<UploadFile> getUploadFiles() {
        return uploadFiles;
    }

    public void setUploadFiles(List<UploadFile> uploadFiles) {
        this.uploadFiles = uploadFiles;
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
