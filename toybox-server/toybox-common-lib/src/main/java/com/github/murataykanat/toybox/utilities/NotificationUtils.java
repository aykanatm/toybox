package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.Notification;
import com.github.murataykanat.toybox.dbo.QNotification;
import com.github.murataykanat.toybox.predicates.ToyboxPredicateBuilder;
import com.github.murataykanat.toybox.predicates.ToyboxStringPath;
import com.github.murataykanat.toybox.repositories.NotificationsRepository;
import com.github.murataykanat.toybox.schema.common.GenericResponse;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.querydsl.core.types.OrderSpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.List;

@Component
public class NotificationUtils {
    private static final Log _logger = LogFactory.getLog(NotificationUtils.class);
    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private NotificationsRepository notificationsRepository;

    @LogEntryExitExecutionTime
    @SuppressWarnings("unchecked")
    public List<Notification> getNotifications(List<SearchCondition> searchConditions, String sortField, String sortType){
        OrderSpecifier<?> order;
        ToyboxPredicateBuilder<Notification> builder = new ToyboxPredicateBuilder().with(searchConditions, Notification.class);

        if(ToyboxConstants.SORT_TYPE_ASCENDING.equalsIgnoreCase(sortType)){
            if("date".equalsIgnoreCase(sortField)){
                order = QNotification.notification1.date.asc();
            }
            else{
                order = new ToyboxStringPath(QNotification.notification1, sortField).asc();
            }
        }
        else if(ToyboxConstants.SORT_TYPE_DESCENDING.equalsIgnoreCase(sortType)){
            if("date".equalsIgnoreCase(sortField)){
                order = QNotification.notification1.date.desc();
            }
            else{
                order = new ToyboxStringPath(QNotification.notification1, sortField).desc();
            }
        }
        else{
            throw new IllegalArgumentException("Sort type '" + sortType + "' is invalid!");
        }

        Iterable<Notification> iterableNotifications = notificationsRepository.findAll(builder.build(), order);
        return Lists.newArrayList(iterableNotifications);
    }
    @LogEntryExitExecutionTime
    public void sendNotification(SendNotificationRequest sendNotificationRequest, HttpSession session) throws Exception {
        try{
            HttpHeaders headers = authenticationUtils.getHeaders(session);
            String loadbalancerUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.NOTIFICATIONS_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.NOTIFICATION_SERVICE_NAME, session, false);
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
