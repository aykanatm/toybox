package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

public class NotificationUtils {
    private static final Log _logger = LogFactory.getLog(NotificationUtils.class);

    private static NotificationUtils notificationUtils;

    private NotificationUtils(){}

    public static NotificationUtils getInstance(){
        if(notificationUtils != null){
            return notificationUtils;
        }

        notificationUtils = new NotificationUtils();
        return notificationUtils;
    }

    @LogEntryExitExecutionTime
    public void sendNotification(SendNotificationRequest sendNotificationRequest, DiscoveryClient discoveryClient, HttpSession session, String serviceName) throws Exception {
        try{
            HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);
            String loadbalancerUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, serviceName);
            HttpEntity<SendNotificationRequest> sendNotificationRequestHttpEntity = new HttpEntity<>(sendNotificationRequest, headers);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForEntity(loadbalancerUrl + "/notifications", sendNotificationRequestHttpEntity, GenericResponse.class);
            _logger.debug("Notification was send successfully!");
        }
        catch (HttpStatusCodeException e){
            JsonObject responseJson = new Gson().fromJson(e.getResponseBodyAsString(), JsonObject.class);
            _logger.error(responseJson.get("message").getAsString(), e);

            throw new Exception("An error occurred while sending a notification. " + responseJson.get("message").getAsString());
        }
    }
}
