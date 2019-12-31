package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Date;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
public interface ShareItem {
    void setCreationDate(Date creationDate);
    Date getCreationDate();
}
