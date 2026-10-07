package com.gk3.demo.orchestrator.api;

import com.gk3.demo.orchestrator.saga.SagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only view of saga state, handy to poll during a demo/portfolio walkthrough while the
 * Outbox relays asynchronously deliver messages between the three services.
 */
@RestController
@RequestMapping("/api/sagas")
public class SagaQueryController {

    private final SagaRepository sagaRepository;

    public SagaQueryController(SagaRepository sagaRepository) {
        this.sagaRepository = sagaRepository;
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<SagaResponse> get(@PathVariable UUID paymentId) {
        return sagaRepository.findById(paymentId)
                .map(SagaResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<SagaResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return sagaRepository.findAll(pageable).map(SagaResponse::from);
    }
}
