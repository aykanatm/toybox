package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import com.github.murataykanat.toybox.schema.search.SearchCondition;

import java.io.Serializable;
import java.util.List;

public class ShareSearchRequest implements Serializable {
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
    @JsonProperty("searchConditions")
    private List<SearchCondition> searchConditions;

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

    public List<SearchRequestFacet> getSearchRequestFacetList() {
        return searchRequestFacetList;
    }

    public void setSearchRequestFacetList(List<SearchRequestFacet> searchRequestFacetList) {
        this.searchRequestFacetList = searchRequestFacetList;
    }

    public List<SearchCondition> getSearchConditions() {
        return searchConditions;
    }

    public void setSearchConditions(List<SearchCondition> searchConditions) {
        this.searchConditions = searchConditions;
    }
}
