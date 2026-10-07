terraform {
  required_version = ">= 1.6"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
    vault = {
      source  = "hashicorp/vault"
      version = "~> 4.0"
    }
  }
}

provider "docker" {
  # Expects the Docker Engine socket to be reachable (on WSL2, this project assumes Terraform is
  # run from inside WSL2, where `docker` already talks to the WSL2 Docker Engine directly).
}

provider "vault" {
  # Reads DB passwords for the postgres containers below from the same local Vault dev server
  # used by docker-compose (see README > "Secrets management"). Requires
  # `docker compose up -d vault vault-seed` to have been run first.
  address = var.vault_address
  token   = var.vault_token
}

resource "docker_network" "saga_net" {
  name = "payment-compliance-saga-net"
}

resource "docker_image" "postgres" {
  name         = "postgres:16-alpine"
  keep_locally = true
}
