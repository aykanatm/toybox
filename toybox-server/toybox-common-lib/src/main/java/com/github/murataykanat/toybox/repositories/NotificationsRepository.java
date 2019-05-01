package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface NotificationsRepository extends JpaRepository<Notification, Integer> {
    @Query(value = "SELECT id, username, from_username, notification, notification_date, is_read FROM notifications", nativeQuery = true)
    List<Notification> getNotifications();

    @Query(value = "SELECT id, username, from_username, notification, notification_date, is_read FROM notifications WHERE username=?1", nativeQuery = true)
    List<Notification> getNotificationsByUsername(String username);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO notifications(username, from_username, notification, notification_date, is_read) VALUES (?1, ?2, ?3, ?4, ?5)", nativeQuery = true)
    void insertNotification(String username, String fromUsername, String notification, Date date, String isRead);
}
