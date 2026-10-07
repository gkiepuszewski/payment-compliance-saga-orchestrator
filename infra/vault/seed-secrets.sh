#!/bin/sh
set -eu

# One-time seed of local demo DB passwords into the ephemeral Vault dev server started by
# `docker compose up -d vault vault-seed` (see docker-compose.yml and README > "Secrets
# management"). Safe to commit: Vault's "-dev" mode is a single-node, in-memory instance that is
# wiped on every restart and never reachable outside your own machine - these are placeholder
# values for local development only and are never used against any real/deployed database.
# Change them here (and re-run this script) if you want different local passwords.

vault kv put secret/payment-service      db_password="demo-payment-pw"       # NOSONAR: ephemeral local Vault dev-mode secret, see comment above
vault kv put secret/compliance-service   db_password="demo-compliance-pw"    # NOSONAR: ephemeral local Vault dev-mode secret, see comment above
vault kv put secret/orchestrator-service db_password="demo-orchestrator-pw"  # NOSONAR: ephemeral local Vault dev-mode secret, see comment above

echo "Seeded DB passwords for payment-service, compliance-service, orchestrator-service into Vault."
