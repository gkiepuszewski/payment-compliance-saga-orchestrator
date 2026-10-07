package com.gk3.demo.compliance.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    default List<OutboxMessage> findBatchOfPending(int batchSize) {
        return findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, batchSize));
    }
}
