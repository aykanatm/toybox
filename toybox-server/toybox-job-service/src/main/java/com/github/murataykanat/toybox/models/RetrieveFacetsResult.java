package com.github.murataykanat.toybox.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class RetrieveFacetsResult implements Serializable {
    @JsonProperty("facets")
    private List<JobFacet> facets;

    public List<JobFacet> getFacets() {
        return facets;
    }

    public void setFacets(List<JobFacet> facets) {
        this.facets = facets;
    }
}
