package com.github.murataykanat.toybox.schema.usergroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.UserGroup;

import java.io.Serializable;
import java.util.List;

public class RetrieveUserGroupsResponse implements Serializable {
    @JsonProperty("usergroups")
    private List<UserGroup> usergroups;
    @JsonProperty("message")
    private String message;

    public List<UserGroup> getUsergroups() {
        return usergroups;
    }

    public void setUsergroups(List<UserGroup> usergroups) {
        this.usergroups = usergroups;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
