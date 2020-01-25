package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class DeleteShareRequest implements Serializable {
    @JsonProperty("type")
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
