package com.github.murataykanat.toybox.models.share;

import java.util.List;

public class SharedContainers {
    private String username;
    private List<String> containerIds;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getContainerIds() {
        return containerIds;
    }

    public void setContainerIds(List<String> containerIds) {
        this.containerIds = containerIds;
    }
}
