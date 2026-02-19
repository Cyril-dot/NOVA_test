package com.novaTech.Nova.Security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(Scheduler.class);

    @Scheduled(fixedRate = 300000)
    public void scheduleTask() {

        logger.info("Fixed rate Scheduler running at {}",
                java.time.LocalDateTime.now());
    }
}
