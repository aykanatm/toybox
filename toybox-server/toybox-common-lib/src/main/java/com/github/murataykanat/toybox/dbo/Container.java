package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "containers")
public class Container implements Serializable, ContainerItem {
    @Id
    @Column(name = "container_id")
    @JsonProperty("id")
    private String id;

    @Column(name = "parent_container_id")
    @JsonProperty("parentId")
    private String parentId;

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

    @Column(name = "is_system")
    @JsonProperty("isSystem")
    private String isSystem;

    @Transient
    private String subscribed;

    @Transient
    private String shared;

    @Transient
    private String sharedByUsername;

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

    @Transient
    @JsonGetter(value = "subscribed")
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getSystem() {
        return isSystem;
    }

    public void setSystem(String isSystem) {
        this.isSystem = isSystem;
    }

    @Transient
    @JsonGetter(value = "shared")
    public String getShared() {
        return shared;
    }

    public void setShared(String shared) {
        this.shared = shared;
    }

    @Transient
    @JsonGetter(value = "sharedByUsername")
    public String getSharedByUsername() {
        return sharedByUsername;
    }

    public void setSharedByUsername(String sharedByUsername) {
        this.sharedByUsername = sharedByUsername;
    }
}
