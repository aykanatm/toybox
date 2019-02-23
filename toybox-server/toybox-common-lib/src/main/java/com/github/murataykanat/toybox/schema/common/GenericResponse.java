package com.github.murataykanat.toybox.schema.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GenericResponse implements Serializable {
    @JsonProperty("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
