package com.github.murataykanat.toybox.schema.share;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.ShareItem;
import com.github.murataykanat.toybox.schema.selection.SelectionContext;

import java.io.Serializable;

public class RetrieveShareResponse implements Serializable {
    @JsonProperty("shareItem")
    private ShareItem shareItem;
    @JsonProperty("selectionContext")
    private SelectionContext selectionContext;
    @JsonProperty("message")
    private String message;

    public ShareItem getShareItem() {
        return shareItem;
    }

    public void setShareItem(ShareItem shareItem) {
        this.shareItem = shareItem;
    }

    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    public void setSelectionContext(SelectionContext selectionContext) {
        this.selectionContext = selectionContext;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
