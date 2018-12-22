package com.github.murataykanat.toybox.models;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class UploadFileLst implements Serializable {
    @JsonProperty("uploadFiles")
    private List<UploadFile> uploadFiles;
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
}
