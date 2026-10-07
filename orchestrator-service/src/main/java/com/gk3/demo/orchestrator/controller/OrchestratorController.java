package com.gk3.demo.orchestrator.controller;

import com.gk3.demo.events.EventEnvelope;
import com.gk3.demo.orchestrator.context.SagaContext;
import com.gk3.demo.orchestrator.inbox.InboxMessage;
import com.gk3.demo.orchestrator.inbox.InboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrchestratorController {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorController.class);

    private final InboxRepository inboxRepository;
    private final OrchestratorDispatcher dispatcher;

    public OrchestratorController(InboxRepository inboxRepository, OrchestratorDispatcher dispatcher) {
        this.inboxRepository = inboxRepository;
        this.dispatcher = dispatcher;
    }

    @PostMapping("/api/sagas/events")
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody EventEnvelope envelope) {
        if (inboxRepository.existsById(envelope.messageId())) {
            log.info("Ignoring already processed message {} ({})", envelope.messageId(), envelope.type());
            return ResponseEntity.ok().build();
        }
        try {
            InboxMessage inbox = InboxMessage.receive(envelope.messageId(), envelope.type());
            inboxRepository.save(inbox);
            // Bind the saga correlation id for the whole processing call graph (JDK 25 ScopedValue).
            SagaContext.runWithSagaId(envelope.sagaId(), () -> dispatcher.dispatch(envelope));
            inbox.markProcessed();
            return ResponseEntity.ok().build();
        } catch (DataIntegrityViolationException alreadyReceivedConcurrently) {
            log.info("Concurrent duplicate delivery of message {} detected, ignoring", envelope.messageId());
            return ResponseEntity.ok().build();
        }
    }
}
