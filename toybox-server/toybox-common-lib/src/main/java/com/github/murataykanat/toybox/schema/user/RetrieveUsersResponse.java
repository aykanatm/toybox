package com.github.murataykanat.toybox.schema.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.User;

import java.io.Serializable;
import java.util.List;

public class RetrieveUsersResponse implements Serializable {
    @JsonProperty("users")
    private List<User> users;
    @JsonProperty("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
