package com.github.murataykanat.toybox.schema.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class NotificationUpdateRequest implements Serializable {
    @JsonProperty("notificationIds")
    private List<Integer> notificationIds;

    @JsonProperty("isRead")
    private String isRead;

    public List<Integer> getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(List<Integer> notificationIds) {
        this.notificationIds = notificationIds;
    }

    public String getIsRead() {
        return isRead;
    }

    public void setIsRead(String isRead) {
        this.isRead = isRead;
    }
}
