variable "databases" {
  description = "One PostgreSQL container per microservice, matching each service's application.yml datasource."
  type = map(object({
    host_port = number
    db_name   = string
    username  = string
    password  = string
  }))

  default = {
    payment = {
      host_port = 5433
      db_name   = "payment_db"
      username  = "payment"
      password  = "payment"
    }
    compliance = {
      host_port = 5434
      db_name   = "compliance_db"
      username  = "compliance"
      password  = "compliance"
    }
    orchestrator = {
      host_port = 5435
      db_name   = "orchestrator_db"
      username  = "orchestrator"
      password  = "orchestrator"
    }
  }
}
