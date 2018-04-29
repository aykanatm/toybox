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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> usersOptional = usersRepository.findByUsername(username);

        // If the username is not found
        usersOptional
                .orElseThrow(() -> new UsernameNotFoundException("Username '" + username + "' not found!"));

        // Return the user as a CustomUserDetails object
        return usersOptional.map(user -> new CustomUserDetails(user)).get();
    }
}
