package com.itsm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Background processing: runs the SLA monitoring cycle on a fixed delay. */
@Component
@ConditionalOnProperty(prefix = "itsm.sla", name = "monitor-enabled", havingValue = "true", matchIfMissing = true)
public class SlaMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitor.class);

    private final SlaMonitoringService service;

    public SlaMonitor(SlaMonitoringService service) {
        this.service = service;
    }

    @Scheduled(initialDelayString = "${itsm.sla.monitor-initial-delay-ms:10000}", fixedDelayString = "${itsm.sla.monitor-interval-ms:15000}")
    public void tick() {
        try {
            var r = service.runCycle();
            if (r.breaches() + r.warnings() + r.escalations() + r.majorIncidents() > 0) {
                log.info("SLA cycle: {}", r);
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            log.debug("SLA cycle skipped due to concurrent update; will retry next tick");
        } catch (RuntimeException e) {
            log.warn("SLA cycle failed: {}", e.getMessage());
        }
    }
}
