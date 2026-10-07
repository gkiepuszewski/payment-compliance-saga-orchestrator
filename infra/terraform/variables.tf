variable "databases" {
  description = "One PostgreSQL container per microservice, matching each service's application.yml datasource. Non-secret settings only - the actual password for each is read from Vault at apply-time via `secret_path` (see postgres.tf's vault_kv_secret_v2 data source), so no credential is ever hard-coded/committed here."
  type = map(object({
    host_port   = number
    db_name     = string
    username    = string
    secret_path = string
  }))

  default = {
    payment = {
      host_port   = 5433
      db_name     = "payment_db"
      username    = "payment"
      secret_path = "payment-service"
    }
    compliance = {
      host_port   = 5434
      db_name     = "compliance_db"
      username    = "compliance"
      secret_path = "compliance-service"
    }
    orchestrator = {
      host_port   = 5435
      db_name     = "orchestrator_db"
      username    = "orchestrator"
      secret_path = "orchestrator-service"
    }
  }
}

variable "vault_address" {
  description = "Address of the local Vault dev server (started via `docker compose up -d vault vault-seed`) that holds the DB passwords used below."
  type    = string
  default = "http://localhost:8200"
}

variable "vault_token" {
  description = "Token for the local Vault dev server. Defaults to the fixed dev-mode root token used everywhere in this repo - safe because that Vault instance is ephemeral, in-memory, and never reachable outside localhost. See README > \"Secrets management\"."
  type      = string
  sensitive = true
  # NOSONAR: fixed root token for the local, ephemeral `vault -dev` server only - see description above.
  default = "dev-only-root-token"
}
