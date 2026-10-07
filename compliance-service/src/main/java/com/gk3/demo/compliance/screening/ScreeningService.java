package com.gk3.demo.compliance.screening;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.compliance.config.OrchestratorProperties;
import com.gk3.demo.compliance.config.ScreeningProperties;
import com.gk3.demo.compliance.outbox.OutboxMessage;
import com.gk3.demo.compliance.outbox.OutboxRepository;
import com.gk3.demo.events.EventType;
import com.gk3.demo.events.payload.PaymentScreenedPayload;
import com.gk3.demo.events.payload.ScreenPaymentCommandPayload;
import com.gk3.demo.events.payload.ScreeningDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Simulates a bank's AML/sanctions screening check (e.g. OFAC/EU sanctions list lookup). A real
 * implementation would call an external screening provider; here payer/payee ids are matched
 * (case-insensitively) against a small configured list to keep the PoC self-contained.
 */
@Service
public class ScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningService.class);

    private final OutboxRepository outboxRepository;
    private final OrchestratorProperties orchestratorProperties;
    private final List<String> sanctionedParties;
    private final ObjectMapper objectMapper;

    public ScreeningService(OutboxRepository outboxRepository, OrchestratorProperties orchestratorProperties,
                             ScreeningProperties screeningProperties, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.orchestratorProperties = orchestratorProperties;
        this.sanctionedParties = screeningProperties.sanctionedParties().stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .toList();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void screen(ScreenPaymentCommandPayload command) {
        boolean payerSanctioned = isSanctioned(command.payerId());
        boolean payeeSanctioned = isSanctioned(command.payeeId());
        ScreeningDecision decision = (payerSanctioned || payeeSanctioned) ? ScreeningDecision.REJECTED : ScreeningDecision.APPROVED;
        String matchedParty = payerSanctioned ? command.payerId() : command.payeeId();
        String reason = decision == ScreeningDecision.REJECTED
                ? "Matched sanctions list: " + matchedParty
                : "No sanctions match";

        log.info("Screened payment {}: {} ({})", command.paymentId(), decision, reason);

        PaymentScreenedPayload result = new PaymentScreenedPayload(command.paymentId(), decision, reason);
        String payloadJson = writeJson(result);
        outboxRepository.save(OutboxMessage.create(
                command.paymentId(), EventType.PAYMENT_SCREENED, payloadJson, orchestratorProperties.url()));
    }

    private boolean isSanctioned(String partyId) {
        return sanctionedParties.contains(partyId.toUpperCase(Locale.ROOT));
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize outbox payload", ex);
        }
    }
}
