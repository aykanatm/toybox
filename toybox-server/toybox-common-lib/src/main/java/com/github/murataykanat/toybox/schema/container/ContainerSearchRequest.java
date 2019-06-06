package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Container;

import java.io.Serializable;

public class ContainerSearchRequest implements Serializable {
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("offset")
    private int offset;
    @JsonProperty("container")
    private Container container;
    @JsonProperty("retrieveTopLevelContainers")
    private String retrieveTopLevelContainers;

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

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public String getRetrieveTopLevelContainers() {
        return retrieveTopLevelContainers;
    }

    public void setRetrieveTopLevelContainers(String retrieveTopLevelContainers) {
        this.retrieveTopLevelContainers = retrieveTopLevelContainers;
    }
}
