package com.gk3.demo.compliance.controller;

import com.gk3.demo.compliance.inbox.InboxMessage;
import com.gk3.demo.compliance.inbox.InboxRepository;
import com.gk3.demo.events.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ComplianceController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceController.class);

    private final InboxRepository inboxRepository;
    private final ComplianceDispatcher dispatcher;

    public ComplianceController(InboxRepository inboxRepository, ComplianceDispatcher dispatcher) {
        this.inboxRepository = inboxRepository;
        this.dispatcher = dispatcher;
    }

    @PostMapping("/api/compliance/screening")
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody EventEnvelope envelope) {
        if (inboxRepository.existsById(envelope.messageId())) {
            log.info("Ignoring already processed message {} ({})", envelope.messageId(), envelope.type());
            return ResponseEntity.ok().build();
        }
        try {
            InboxMessage inbox = InboxMessage.receive(envelope.messageId(), envelope.type());
            inboxRepository.save(inbox);
            dispatcher.dispatch(envelope);
            inbox.markProcessed();
            return ResponseEntity.ok().build();
        } catch (DataIntegrityViolationException _) {
            log.info("Concurrent duplicate delivery of message {} detected, ignoring", envelope.messageId());
            return ResponseEntity.status(HttpStatus.OK).build();
        }
    }
}
