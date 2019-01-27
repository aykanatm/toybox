package com.github.murataykanat.toybox.schema.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.dbo.User;

import java.io.Serializable;

public class UserResponse implements Serializable {
    @JsonProperty("user")
    private User user;
    @JsonProperty("message")
    private String message;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
