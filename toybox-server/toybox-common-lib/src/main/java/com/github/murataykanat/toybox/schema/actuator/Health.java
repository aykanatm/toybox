package com.github.murataykanat.toybox.schema.actuator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Health implements Serializable {
    @JsonProperty("status")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
