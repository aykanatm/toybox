package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.AssetUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AssetUserRepository extends JpaRepository<AssetUser, String> {
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO asset_user(asset_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
    int insertSubscriber(String assetId, int userId);
}
