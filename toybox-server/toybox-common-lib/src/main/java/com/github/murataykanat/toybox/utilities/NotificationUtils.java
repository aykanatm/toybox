package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;

@Component
public class NotificationUtils {
    private static final Log _logger = LogFactory.getLog(NotificationUtils.class);
    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;

    @LogEntryExitExecutionTime
    public void sendNotification(SendNotificationRequest sendNotificationRequest, HttpSession session) throws Exception {
        try{
            HttpHeaders headers = authenticationUtils.getHeaders(session);
            String loadbalancerUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.NOTIFICATIONS_LOAD_BALANCER_SERVICE_NAME);
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
