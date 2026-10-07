package com.gk3.demo.orchestrator.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SagaRepository extends JpaRepository<SagaInstance, UUID> {
}
