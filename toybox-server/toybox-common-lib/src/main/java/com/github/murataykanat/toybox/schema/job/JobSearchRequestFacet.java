package com.github.murataykanat.toybox.schema.job;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class JobSearchRequestFacet implements Serializable {
    @JsonProperty("fieldName")
    private String fieldName;
    @JsonProperty("fieldValue")
    private String fieldValue;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
