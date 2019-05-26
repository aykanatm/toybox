package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "containers")
public class Container {
    @Id
    @Column(name = "container_id")
    @JsonProperty("id")
    private String id;

    @Column(name = "container_name")
    @JsonProperty("name")
    private String name;

    @Column(name = "container_created_by_username")
    @JsonProperty("createdByUsername")
    private String createdByUsername;

    @Column(name = "container_creation_date")
    @JsonProperty("creationDate")
    private Date creationDate;

    @Column(name = "deleted")
    @JsonProperty("deleted")
    private String deleted;

    @Transient
    private String subscribed;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = Asset.class)
    @JoinTable(name = "container_asset", joinColumns = @JoinColumn(name = "container_id"), inverseJoinColumns = @JoinColumn(name = "asset_id"))
    @JsonIgnore
    private Set<Asset> assets;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = User.class)
    @JoinTable(name = "container_user", joinColumns = @JoinColumn(name = "container_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnore
    private Set<User> subscribers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    public String getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(String subscribed) {
        this.subscribed = subscribed;
    }

    public Set<Asset> getAssets() {
        return assets;
    }

    public void setAssets(Set<Asset> assets) {
        this.assets = assets;
    }

    public Set<User> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<User> subscribers) {
        this.subscribers = subscribers;
    }
}
