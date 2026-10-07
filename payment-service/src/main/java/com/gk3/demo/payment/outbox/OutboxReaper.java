package com.gk3.demo.payment.outbox;

import com.gk3.demo.payment.config.OutboxReaperProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically scans for {@code FAILED} outbox rows (delivery exhausted {@code maxAttempts}) and
 * raises an alert-level log entry for each one, so they don't silently sit forever. This is the
 * "dead-letter monitor" half of the Outbox pattern; {@link OutboxAdminController} is the
 * operator-triggered "replay" half.
 */
@Component
public class OutboxReaper {

    private static final Logger log = LoggerFactory.getLogger(OutboxReaper.class);

    private final OutboxRepository outboxRepository;

    public OutboxReaper(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Scheduled(fixedDelayString = "${outbox.reaper.fixed-delay:60000}")
    public void alertOnFailedMessages() {
        List<OutboxMessage> failed = outboxRepository.findByStatus(OutboxStatus.FAILED);
        if (failed.isEmpty()) {
            return;
        }
        log.error("{} outbox message(s) in payment-service have exhausted their delivery attempts "
                        + "and need manual replay (POST /api/admin/outbox/{{messageId}}/replay): {}",
                failed.size(), failed.stream().map(OutboxMessage::getId).toList());
    }
}
