package com.gk3.demo.payment.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(OutboxStatus status, org.springframework.data.domain.Pageable pageable);

    default List<OutboxMessage> findBatchOfPending(int batchSize) {
        return findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, org.springframework.data.domain.PageRequest.of(0, batchSize));
    }
}
