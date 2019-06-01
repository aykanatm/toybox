package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ContainerSearchRequest implements Serializable {
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("offset")
    private int offset;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
