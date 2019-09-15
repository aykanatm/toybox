package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserGroupsRepository extends JpaRepository<UserGroup, Integer> {
    @Query(value = "SELECT usergroup_id, name FROM usergroups", nativeQuery = true)
    List<UserGroup> findAll();
}
