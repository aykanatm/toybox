package com.github.murataykanat.toybox.schema.asset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;

import java.io.Serializable;

public class MoveAssetRequest implements Serializable {
    @JsonProperty("selectionContext")
    private SelectionContext selectionContext;
    @JsonProperty("containerId")
    private String containerId;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    public void setSelectionContext(SelectionContext selectionContext) {
        this.selectionContext = selectionContext;
    }
}
