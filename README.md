# payment-compliance-saga-orchestrator

Portfolio PoC: a **Payment + Compliance (AML/sanctions screening) saga**, orchestrated by a
hand-written Java saga orchestrator that talks to two independently deployable Spring Boot
microservices purely over **REST**, using the **transactional Outbox/Inbox pattern** for
reliable, idempotent, at-least-once messaging — **no message broker involved**.

This is intentionally *not* another "Step Functions calling DynamoDB" demo. The orchestrator is a
real microservice with its own database; `payment-service` and `compliance-service` are separate
deployable units with their own databases, and all cross-service communication happens over HTTP.

## Why this design

| Decision | Rationale |
|---|---|
| Custom orchestrator (not AWS Step Functions) | Full control over the saga state machine, no LocalStack/Step-Functions-Local limitations, portable to Kubernetes unchanged. |
| REST + Outbox/Inbox (not a broker) | No infra beyond PostgreSQL; still gets at-least-once delivery + idempotency + a queryable audit trail (`outbox_messages` / `inbox_messages`), which fits the compliance domain well. |
| Separate DB per service | Each service is a real bounded context, not just a table in a shared DB — the orchestrator never touches `payment_db` or `compliance_db` directly. |
| JDK 25 | Uses finalized **Scoped Values** (JEP 506) to propagate the saga correlation id through the call stack without threading it through every method signature, plus **virtual threads** for request handling. |
| Terraform (docker provider) | Infra-as-code for local dev, matching the "everything as code" ethos, without needing AWS/LocalStack for this particular PoC. |

## Modules

```
common-events/         Shared message contracts (Java records), no framework dependencies
payment-service/        :8081  Owns Payment aggregate + its own Postgres DB (payment_db)
compliance-service/     :8082  Simulated AML/sanctions screening + its own Postgres DB (compliance_db)
orchestrator-service/   :8080  Saga state machine + its own Postgres DB (orchestrator_db)
infra/terraform/        Spins up 3 local PostgreSQL containers via the Docker provider
```

## Saga flow

```
1. POST /api/payments (payment-service)
     -> Payment saved as PENDING
     -> PaymentCreated event written to payment-service's outbox (same local transaction)

2. OutboxRelay (payment-service) delivers PaymentCreated -> orchestrator-service /api/sagas/events
     -> orchestrator starts a SagaInstance (STARTED -> AWAITING_SCREENING)
     -> ScreenPaymentCommand written to orchestrator's outbox

3. OutboxRelay (orchestrator-service) delivers ScreenPaymentCommand -> compliance-service /api/compliance/screening
     -> compliance-service screens payer/payee against a configured sanctions list
     -> PaymentScreened (APPROVED | REJECTED) written to compliance-service's outbox

4. OutboxRelay (compliance-service) delivers PaymentScreened -> orchestrator-service /api/sagas/events
     -> APPROVED: saga COMPLETED, ConfirmPaymentCommand enqueued
     -> REJECTED: saga COMPENSATED, CancelPaymentCommand enqueued (the saga's compensating action)

5. OutboxRelay (orchestrator-service) delivers Confirm/CancelPaymentCommand -> payment-service /api/payments/confirmation
     -> Payment transitions PENDING -> CONFIRMED or PENDING -> CANCELLED
```

Every hop is an HTTP POST carrying an `EventEnvelope { messageId, type, sagaId,
sourceService, occurredAt, payload }` to the receiving service's Inbox endpoint. Each service
exposes a single, domain-meaningful path rather than a generic `/api/inbox`, so the HTTP contract
reads as "what does this service do" rather than leaking the Inbox pattern name: `/api/sagas/events`
(orchestrator-service, receives PaymentCreated/PaymentScreened), `/api/compliance/screening`
(compliance-service, receives ScreenPaymentCommand), `/api/payments/confirmation` (payment-service,
receives the saga's final Confirm/Cancel decision). The receiving Inbox persists `messageId` before
processing, so redelivery after a timeout/5xx is always safe to retry.

## Reliable messaging: Outbox/Inbox, not a broker

- **Outbox**: writing the business state change (e.g. `Payment` -> PENDING) and the outgoing event
  row happen in the *same local database transaction* — either both commit or neither does.
- **Relay**: a `@Scheduled` poller reads `PENDING` outbox rows and POSTs them to the target
  service. Non-2xx responses / exceptions leave the row `PENDING` (retried next tick) or mark it
  `FAILED` after `outbox.relay.max-attempts` (dead-letter, needs manual replay).
- **Inbox**: the receiver persists `messageId` as its primary key *before* processing. A duplicate
  delivery of an already-processed message is detected via `existsById` (or a `DataIntegrityViolationException`
  on a concurrent race) and is a no-op — this is what makes retries safe.

## JDK 25 features in `orchestrator-service`

- `spring.threads.virtual.enabled=true` — Tomcat handles each `/api/sagas/events` request on a virtual thread.
- `com.gk3.demo.orchestrator.context.SagaContext` wraps a `ScopedValue<UUID>` bound for the whole
  processing of an inbound message (`SagaContext.runWithSagaId(sagaId, () -> dispatcher.dispatch(...))`).
  Deep in the call graph, `SagaOrchestrationService` reads the current saga id back out with
  `SagaContext.currentSagaId()` for correlated logging — without `sagaId` being passed as an
  explicit parameter to every method. Unlike a `ThreadLocal`, the binding is immutable for that
  dynamic scope and cannot leak past it.

## Running locally

Requires: JDK 25, Docker Engine (tested against Docker-in-WSL2), Terraform >= 1.6.

```powershell
# 1. Provision the 3 PostgreSQL containers (run from inside WSL2 if Docker only lives there)
cd infra/terraform
terraform init
terraform apply -auto-approve

# 2. Build & install the reactor (common-events must be installed before the services resolve it)
cd ../..
$env:JAVA_HOME = "<path to JDK 25>"
./mvnw install -DskipTests

# 3. Run each service (separate terminals), from inside its own module directory
cd orchestrator-service;  ../mvnw spring-boot:run   # :8080
cd payment-service;       ../mvnw spring-boot:run   # :8081
cd compliance-service;    ../mvnw spring-boot:run   # :8082
```

### Try it

```powershell
# Happy path: approved -> payment is CONFIRMED
$p = Invoke-RestMethod http://localhost:8081/api/payments -Method Post -ContentType "application/json" `
     -Body '{"payerId":"ALICE","payeeId":"BOB","amount":150.00,"currency":"EUR"}'
Start-Sleep 5
Invoke-RestMethod "http://localhost:8081/api/payments/$($p.id)"   # status: CONFIRMED
Invoke-RestMethod "http://localhost:8080/api/sagas/$($p.id)"      # state: COMPLETED

# Compensation path: rejected -> payment is CANCELLED
$p = Invoke-RestMethod http://localhost:8081/api/payments -Method Post -ContentType "application/json" `
     -Body '{"payerId":"SANCTIONED","payeeId":"BOB","amount":999.00,"currency":"USD"}'
Start-Sleep 5
Invoke-RestMethod "http://localhost:8081/api/payments/$($p.id)"   # status: CANCELLED
Invoke-RestMethod "http://localhost:8080/api/sagas/$($p.id)"      # state: COMPENSATED
```

`compliance-service`'s toy sanctions list (`compliance.sanctioned-parties` in its
`application.yml`) rejects any payer/payee id matching (case-insensitively) `SANCTIONED`,
`BLOCKED-PARTY` or `OFAC-TEST`.

### Tear down

```bash
cd infra/terraform && terraform destroy -auto-approve
```

## Possible follow-ups (not implemented, out of scope for this PoC)

- Flyway/Liquibase migrations instead of `hibernate.ddl-auto=update`.
- A scheduled reaper to alert/replay `FAILED` outbox rows.
- Containerizing the three services themselves (Dockerfiles) + a Kubernetes manifest, to show the
  orchestrator running unchanged outside this Terraform/local-JVM setup.
