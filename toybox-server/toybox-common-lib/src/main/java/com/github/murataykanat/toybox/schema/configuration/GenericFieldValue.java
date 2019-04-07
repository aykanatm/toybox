package com.github.murataykanat.toybox.schema.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GenericFieldValue implements Serializable {
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
