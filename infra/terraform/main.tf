terraform {
  required_version = ">= 1.6"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {
  # Expects the Docker Engine socket to be reachable (on WSL2, this project assumes Terraform is
  # run from inside WSL2, where `docker` already talks to the WSL2 Docker Engine directly).
}

resource "docker_network" "saga_net" {
  name = "payment-compliance-saga-net"
}

resource "docker_image" "postgres" {
  name         = "postgres:16-alpine"
  keep_locally = true
}
