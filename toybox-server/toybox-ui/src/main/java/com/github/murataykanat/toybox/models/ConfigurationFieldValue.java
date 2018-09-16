package com.github.murataykanat.toybox.models;

import org.codehaus.jackson.annotate.JsonProperty;

public class ConfigurationFieldValue {
    @JsonProperty("value")
    private String value;

    public ConfigurationFieldValue(String value){
        this.value = value;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }
}
