package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.Notification;
import com.github.murataykanat.toybox.dbo.QNotification;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.SingleValueBinding;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface NotificationsRepository extends JpaRepository<Notification, Integer>, QueryDslPredicateExecutor<Notification>, QuerydslBinderCustomizer<QNotification> {
    @Override
    default public void customize(QuerydslBindings bindings, QNotification root) {
        bindings.bind(String.class)
                .first((SingleValueBinding<StringPath, String>) StringExpression::containsIgnoreCase);
    }

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO notifications(username, from_username, notification, notification_date, is_read) VALUES (?1, ?2, ?3, ?4, ?5)", nativeQuery = true)
    void insertNotification(String username, String fromUsername, String notification, Date date, String isRead);

    @Transactional
    @Modifying
    @Query(value = "UPDATE notifications SET is_read=:isRead WHERE id IN :notificationIds", nativeQuery = true)
    int updateNotifications(@Param("isRead") String isRead, @Param("notificationIds") List<Integer> notificationIds);

    @Transactional
    @Modifying
    @Query(value = "UPDATE notifications SET is_read=:isRead", nativeQuery = true)
    int updateAllNotifications(@Param("isRead") String isRead);
}
