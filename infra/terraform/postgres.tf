resource "docker_volume" "postgres_data" {
  for_each = var.databases
  name     = "payment-compliance-saga-${each.key}-pgdata"
}

resource "docker_container" "postgres" {
  for_each = var.databases

  name    = "payment-compliance-saga-${each.key}-db"
  image   = docker_image.postgres.image_id
  restart = "unless-stopped"

  env = [
    "POSTGRES_DB=${each.value.db_name}",
    "POSTGRES_USER=${each.value.username}",
    "POSTGRES_PASSWORD=${each.value.password}",
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
