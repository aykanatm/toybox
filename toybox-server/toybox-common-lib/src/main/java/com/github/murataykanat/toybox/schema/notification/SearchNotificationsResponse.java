package com.github.murataykanat.toybox.schema.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.Notification;

import java.io.Serializable;
import java.util.List;

public class SearchNotificationsResponse implements Serializable {
    @JsonProperty("notifications")
    private List<Notification> notifications;
    @JsonProperty("message")
    private String message;

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
