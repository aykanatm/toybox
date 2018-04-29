package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.models.User;
import com.github.murataykanat.toybox.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;

@RestController
public class AuthorizationController {
    @Autowired
    private UsersRepository usersRepository;

    // Returns the currently authenticated user
    @RequestMapping({"/user", "/me"})
    public User user(Principal user){
        Optional<User> optionalUser = usersRepository.findByUsername(user.getName());
        optionalUser
                .orElseThrow(() -> new UsernameNotFoundException("Username '" + user.getName() + "' not found!"));
        return optionalUser.get();
    }
}