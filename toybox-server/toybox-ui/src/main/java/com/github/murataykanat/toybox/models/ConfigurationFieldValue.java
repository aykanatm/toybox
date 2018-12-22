package com.github.murataykanat.toybox.models;

import org.codehaus.jackson.annotate.JsonProperty;

public class ConfigurationFieldValue {
    @JsonProperty("value")
    private String value;
    @JsonProperty("message")
    private String message;

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
