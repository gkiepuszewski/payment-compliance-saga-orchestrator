package com.gk3.demo.payment.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxRepository extends JpaRepository<InboxMessage, UUID> {
}
