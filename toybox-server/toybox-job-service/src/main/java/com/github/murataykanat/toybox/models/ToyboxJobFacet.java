package com.github.murataykanat.toybox.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

public class ToyboxJobFacet implements Serializable {
    @JsonProperty("name")
    private String name;
    @JsonProperty("lookups")
    private List<String> lookups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLookups() {
        return lookups;
    }

    public void setLookups(List<String> lookups) {
        this.lookups = lookups;
    }
}