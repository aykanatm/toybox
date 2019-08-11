package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ExternalShareResponse implements Serializable {
    @JsonProperty("url")
    private String url;
    @JsonProperty("message")
    private String message;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
