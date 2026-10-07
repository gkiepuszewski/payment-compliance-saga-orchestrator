resource "docker_volume" "postgres_data" {
  for_each = var.databases
  name     = "payment-compliance-saga-${each.key}-pgdata"
}

# Each service's DB password, read from the same Vault KV v2 secret that
# infra/vault/seed-secrets.sh seeds and that VaultEnvironmentPostProcessor reads at application
# startup - Vault is the single source of truth for every DB password in this repo.
data "vault_kv_secret_v2" "db_password" {
  for_each = var.databases
  mount    = "secret"
  name     = each.value.secret_path
}

resource "docker_container" "postgres" {
  for_each = var.databases

  name    = "payment-compliance-saga-${each.key}-db"
  image   = docker_image.postgres.image_id
  restart = "unless-stopped"

  env = [
    "POSTGRES_DB=${each.value.db_name}",
    "POSTGRES_USER=${each.value.username}",
    "POSTGRES_PASSWORD=${data.vault_kv_secret_v2.db_password[each.key].data["db_password"]}",
  ]

  ports {
    internal = 5432
    external = each.value.host_port
  }

  networks_advanced {
    name = docker_network.saga_net.name
  }

  volumes {
    volume_name    = docker_volume.postgres_data[each.key].name
    container_path = "/var/lib/postgresql/data"
  }

  healthcheck {
    test     = ["CMD-SHELL", "pg_isready -U ${each.value.username} -d ${each.value.db_name}"]
    interval = "5s"
    timeout  = "3s"
    retries  = 10
  }
}
