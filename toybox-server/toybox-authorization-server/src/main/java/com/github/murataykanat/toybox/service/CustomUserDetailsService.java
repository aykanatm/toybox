package com.github.murataykanat.toybox.service;

import com.github.murataykanat.toybox.models.CustomUserDetails;
import com.github.murataykanat.toybox.models.User;
import com.github.murataykanat.toybox.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Optional<User> usersOptional = usersRepository.findByUsername(username);

        // If the username is found
        if(usersOptional.isPresent()){
            // Return the user as a CustomUserDetails object
            User user = usersOptional.get();
            return new CustomUserDetails(user);
        }
        else{
            throw new UsernameNotFoundException("Username '" + username + "' not found!");
        }
    }
}
