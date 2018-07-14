package com.github.murataykanat.toybox.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class UserController {
    // Returns the currently authenticated user
    @RequestMapping({"/user", "/me"})
    public Principal user(Principal user){
        return user;
    }
}