package com.github.murataykanat.toybox.repository;

import com.github.murataykanat.toybox.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, Integer>{
    // findBy automatically translates "Username" into "username".
    // The User class must have a field annotated "username" in order for this to work
    Optional<User> findByUsername(String username);
}
