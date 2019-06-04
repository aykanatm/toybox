package com.github.murataykanat.toybox.schema.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.ContainerItem;
import com.github.murataykanat.toybox.schema.common.Facet;

import java.io.Serializable;
import java.util.List;

public class RetrieveContainerContentsResult implements Serializable {
    @JsonProperty("containerItems")
    private List<ContainerItem> containerItems;
    @JsonProperty("totalRecords")
    private int totalRecords;
    @JsonProperty("facets")
    private List<Facet> facets;
    @JsonProperty("message")
    private String message;

    public List<ContainerItem> getContainerItems() {
        return containerItems;
    }

    public void setContainerItems(List<ContainerItem> containerItems) {
        this.containerItems = containerItems;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }
}
