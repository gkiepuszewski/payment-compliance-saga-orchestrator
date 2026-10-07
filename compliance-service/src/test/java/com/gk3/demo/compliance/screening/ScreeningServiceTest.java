package com.gk3.demo.compliance.screening;

import com.gk3.demo.compliance.config.OrchestratorProperties;
import com.gk3.demo.compliance.config.ScreeningProperties;
import com.gk3.demo.compliance.outbox.OutboxMessage;
import com.gk3.demo.compliance.outbox.OutboxRepository;
import com.gk3.demo.events.EventType;
import com.gk3.demo.events.payload.ScreenPaymentCommandPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    private static final String ORCHESTRATOR_URL = "http://orchestrator:8080/api/sagas/events";

    @Mock
    private OutboxRepository outboxRepository;

    private ScreeningService screeningService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        OrchestratorProperties orchestratorProperties = new OrchestratorProperties(ORCHESTRATOR_URL);
        ScreeningProperties screeningProperties = new ScreeningProperties(List.of("SANCTIONED", "BLOCKED-PARTY", "OFAC-TEST"));
        screeningService = new ScreeningService(outboxRepository, orchestratorProperties, screeningProperties, objectMapper);
    }

    @Test
    void approvesPaymentWhenNeitherPartyIsSanctioned() {
        ScreenPaymentCommandPayload command = new ScreenPaymentCommandPayload(
                UUID.randomUUID(), "alice", "bob", BigDecimal.TEN, "EUR");

        screeningService.screen(command);

        OutboxMessage outboxMessage = captureOutboxMessage();
        assertThat(outboxMessage.getEventType()).isEqualTo(EventType.PAYMENT_SCREENED);
        assertThat(outboxMessage.getPayloadJson()).contains("\"APPROVED\"");
    }

    @Test
    void rejectsPaymentWhenPayerIsSanctioned() {
        ScreenPaymentCommandPayload command = new ScreenPaymentCommandPayload(
                UUID.randomUUID(), "SANCTIONED", "bob", BigDecimal.TEN, "EUR");

        screeningService.screen(command);

        OutboxMessage outboxMessage = captureOutboxMessage();
        assertThat(outboxMessage.getPayloadJson())
                .contains("\"REJECTED\"")
                .contains("Matched sanctions list: SANCTIONED");
    }

    @Test
    void rejectsPaymentWhenPayeeIsSanctioned() {
        ScreenPaymentCommandPayload command = new ScreenPaymentCommandPayload(
                UUID.randomUUID(), "alice", "blocked-party", BigDecimal.TEN, "EUR");

        screeningService.screen(command);

        OutboxMessage outboxMessage = captureOutboxMessage();
        assertThat(outboxMessage.getPayloadJson()).contains("\"REJECTED\"");
    }

    @Test
    void sanctionsMatchingIsCaseInsensitive() {
        ScreenPaymentCommandPayload command = new ScreenPaymentCommandPayload(
                UUID.randomUUID(), "sanctioned", "bob", BigDecimal.TEN, "EUR");

        screeningService.screen(command);

        OutboxMessage outboxMessage = captureOutboxMessage();
        assertThat(outboxMessage.getPayloadJson()).contains("\"REJECTED\"");
    }

    @Test
    void outboxMessageTargetsOrchestratorInbox() {
        ScreenPaymentCommandPayload command = new ScreenPaymentCommandPayload(
                UUID.randomUUID(), "alice", "bob", BigDecimal.TEN, "EUR");

        screeningService.screen(command);

        assertThat(captureOutboxMessage().getTargetUrl()).isEqualTo(ORCHESTRATOR_URL);
    }

    private OutboxMessage captureOutboxMessage() {
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());
        return captor.getValue();
    }
}
