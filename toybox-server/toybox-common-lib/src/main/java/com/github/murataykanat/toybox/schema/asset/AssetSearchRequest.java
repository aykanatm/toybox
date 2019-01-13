package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;

import java.io.Serializable;
import java.util.List;

public class AssetSearchRequest implements Serializable {
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("offset")
    private int offset;
    @JsonProperty("sortType")
    private String sortType;
    @JsonProperty("sortColumn")
    private String sortColumn;
    @JsonProperty("assetSearchRequestFacetList")
    private List<SearchRequestFacet> searchRequestFacetList;

    public List<SearchRequestFacet> getAssetSearchRequestFacetList() {
        return searchRequestFacetList;
    }

    public void setAssetSearchRequestFacetList(List<SearchRequestFacet> assetSearchRequestFacetList) {
        this.searchRequestFacetList = assetSearchRequestFacetList;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }
}