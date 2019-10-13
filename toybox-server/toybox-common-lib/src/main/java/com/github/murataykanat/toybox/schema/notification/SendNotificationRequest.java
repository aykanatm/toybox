package com.github.murataykanat.toybox.schema.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class SendNotificationRequest implements Serializable {
    @JsonProperty("fromUser")
    private String fromUsername;
    @JsonProperty("toUser")
    private String toUsername;
    @JsonProperty("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToUsername() {
        return toUsername;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }
}
