package com.github.murataykanat.toybox.schema.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class SearchNotificationsRequest implements Serializable {
    @JsonProperty("id")
    private int id;
    @JsonProperty("username")
    private String username;
    @JsonProperty("fromUsername")
    private String from;
    @JsonProperty("content")
    private String content;
    @JsonProperty("notificationDate")
    private Date date;
    @JsonProperty("isRead")
    private String isRead;
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("offset")
    private int offset;
    @JsonProperty("searchRequestFacetList")
    private List<SearchRequestFacet> searchRequestFacetList;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String notification) {
        this.content = notification;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIsRead() {
        return isRead;
    }

    public void setIsRead(String isRead) {
        this.isRead = isRead;
    }

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

    public List<SearchRequestFacet> getSearchRequestFacetList() {
        return searchRequestFacetList;
    }

    public void setSearchRequestFacetList(List<SearchRequestFacet> searchRequestFacetList) {
        this.searchRequestFacetList = searchRequestFacetList;
    }
}
