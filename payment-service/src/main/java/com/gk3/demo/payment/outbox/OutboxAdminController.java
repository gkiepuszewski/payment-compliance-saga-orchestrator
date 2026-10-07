package com.gk3.demo.payment.outbox;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Operator endpoints for the Outbox dead-letter workflow: list rows that exhausted their delivery
 * attempts ({@link OutboxReaper} alerts on these via logs) and replay them (reset to {@code
 * PENDING} so the next {@link OutboxRelay} tick retries delivery).
 */
@RestController
@RequestMapping("/api/admin/outbox")
public class OutboxAdminController {

    private final OutboxRepository outboxRepository;

    public OutboxAdminController(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @GetMapping("/failed")
    public List<OutboxMessage> listFailed() {
        return outboxRepository.findByStatus(OutboxStatus.FAILED);
    }

    @PostMapping("/{messageId}/replay")
    @Transactional
    public ResponseEntity<Void> replay(@PathVariable UUID messageId) {
        return outboxRepository.findById(messageId)
                .map(message -> {
                    message.resetForRetry();
                    outboxRepository.save(message);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/replay-failed")
    @Transactional
    public ResponseEntity<Integer> replayAllFailed() {
        List<OutboxMessage> failed = outboxRepository.findByStatus(OutboxStatus.FAILED);
        failed.forEach(OutboxMessage::resetForRetry);
        outboxRepository.saveAll(failed);
        return ResponseEntity.ok(failed.size());
    }
}
