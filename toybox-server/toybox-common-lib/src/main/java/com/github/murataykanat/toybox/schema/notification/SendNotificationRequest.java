package com.github.murataykanat.toybox.schema.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.dbo.User;

import java.io.Serializable;

public class SendNotificationRequest implements Serializable {
    @JsonProperty("fromUser")
    private User fromUser;
    @JsonProperty("id")
    private String id;
    @JsonProperty("message")
    private String message;
    @JsonProperty("isAsset")
    private boolean isAsset;

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean getIsAsset() {
        return isAsset;
    }

    public void setIsAsset(boolean isAsset) {
        this.isAsset = isAsset;
    }
}
