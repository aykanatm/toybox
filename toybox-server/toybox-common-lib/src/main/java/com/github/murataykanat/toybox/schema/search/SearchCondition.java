package com.github.murataykanat.toybox.schema.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class SearchCondition implements Serializable {
    @JsonProperty("keyword")
    private String keyword;
    @JsonProperty("field")
    private String field;
    @JsonProperty("operator")
    private String operator;
    @JsonProperty("dataType")
    private String dataType;
    @JsonProperty("booleanOperator")
    private String booleanOperator;
    @JsonProperty("innerFilter")
    private SearchCondition innerFilter;

    public SearchCondition(){}

    public SearchCondition(String field, String operator, String keyword, String dataType, String booleanOperator){
        this.field = field;
        this.operator = operator;
        this.keyword = keyword;
        this.dataType = dataType;
        this.booleanOperator = booleanOperator;
    }

    public SearchCondition(String field, String operator, String keyword, String dataType, String booleanOperator, SearchCondition innerFilter){
        this.field = field;
        this.operator = operator;
        this.keyword = keyword;
        this.dataType = dataType;
        this.booleanOperator = booleanOperator;
        this.innerFilter = innerFilter;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getBooleanOperator() {
        return booleanOperator;
    }

    public void setBooleanOperator(String booleanOperator) {
        this.booleanOperator = booleanOperator;
    }

    public SearchCondition getInnerFilter() {
        return innerFilter;
    }

    public void setInnerFilter(SearchCondition innerFilter) {
        this.innerFilter = innerFilter;
    }
}