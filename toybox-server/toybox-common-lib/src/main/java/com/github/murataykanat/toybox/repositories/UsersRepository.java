package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<User, Integer>{
    // findBy automatically translates "Username" into "username".
    // The User class must have a field annotated "username" in order for this to work
    Optional<User> findByUsername(String username);
    @Query(value = "SELECT user_id, password, email, enabled, account_non_expired, account_non_locked, credentials_non_expired, lastname, name, username, avatar_path FROM users WHERE username=?1", nativeQuery = true)
    List<User> findUsersByUsername(String username);
    @Query(value = "SELECT user_id, password, email, enabled, account_non_expired, account_non_locked, credentials_non_expired, lastname, name, username, avatar_path FROM users WHERE user_id=?1", nativeQuery = true)
    List<User> findUsersByUserId(int userId);
    @Query(value = "SELECT user_id, password, email, enabled, account_non_expired, account_non_locked, credentials_non_expired, lastname, name, username, avatar_path FROM users", nativeQuery = true)
    List<User> findAll();
}
