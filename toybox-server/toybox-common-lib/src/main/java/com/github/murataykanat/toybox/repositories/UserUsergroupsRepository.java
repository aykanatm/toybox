package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.UserUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserUsergroupsRepository extends JpaRepository<UserUserGroup, Integer> {
    @Query(value = "SELECT usergroup_id, user_id FROM user_usergroup WHERE usergroup_id=?1", nativeQuery = true)
    List<UserUserGroup> findUserUserGroupByUserGroupId(int userGroupId);
}
