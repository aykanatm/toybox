package com.github.murataykanat.toybox.listeners;

import com.github.murataykanat.toybox.schema.notification.Notification;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

@Component
public class NotificationListener {
    private static final Log _logger = LogFactory.getLog(NotificationListener.class);
    public void receiveNotification(Notification notification){
        _logger.debug("receiveNotification() >>");
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        _logger.debug("Username: " + notification.getUsername());
        _logger.debug("Notification: " + notification.getNotification());
        _logger.debug("Date: " + formatter.format(notification.getDate()));
        _logger.debug("<< receiveNotification()");
    }
}
