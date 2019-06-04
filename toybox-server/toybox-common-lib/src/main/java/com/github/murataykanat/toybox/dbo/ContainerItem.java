package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
public interface ContainerItem {}
