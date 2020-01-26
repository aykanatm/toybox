package com.github.murataykanat.toybox.schedulers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.ExternalShare;
import com.github.murataykanat.toybox.dbo.InternalShare;
import com.github.murataykanat.toybox.utilities.ShareUtils;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SchedulerLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class ShareCleanupScheduler {
    @Autowired
    private ShareUtils shareUtils;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    @LogEntryExitExecutionTime
    @Scheduled(cron = "0 1 * * *")
    @SchedulerLock(name = "TaskScheduler_cleanupExpiredShares", lockAtLeastForString = "PT5M", lockAtMostForString = "PT14M")
    public void cleanupExpiredShares() throws IOException {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        List<InternalShare> expiredInternalShares = shareUtils.getExpiredInternalShares(now);
        List<ExternalShare> expiredExternalShares = shareUtils.getExpiredExternalShares(now);

        for(InternalShare internalShare: expiredInternalShares){
            shareUtils.deleteInternalShare(internalShare.getId());
        }

        for(ExternalShare externalShare: expiredExternalShares){
            shareUtils.deleteExternalShare(externalShare.getId());
        }
    }
}
