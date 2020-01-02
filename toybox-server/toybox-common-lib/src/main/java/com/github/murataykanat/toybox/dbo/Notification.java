package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDataType;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "notifications")
public class Notification implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @JsonProperty("id")
    private int id;

    @Column(name = "username")
    @JsonProperty("username")
    private String username;

    @Column(name = "from_username")
    @JsonProperty("fromUsername")
    @FacetColumnName("Username")
    private String from;

    @Column(name = "notification")
    @JsonProperty("notification")
    private String notification;

    @Column(name = "notification_date")
    @JsonProperty("notificationDate")
    @FacetColumnName("Date")
    @FacetDataType(value = "Date")
    @FacetDefaultLookup(values = {"Next 30+ days", "Next 30 days", "Next 7 days", "Today","Past 7 days","Past 30 days", "Past 30+ days"})
    private Date date;

    @Column(name = "is_read")
    @JsonProperty("isRead")
    @FacetColumnName("Read")
    private String isRead;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
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
}
