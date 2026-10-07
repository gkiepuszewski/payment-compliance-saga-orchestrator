output "jdbc_urls" {
  description = "JDBC URL per service, matching each service's application.yml datasource.url"
  value = {
    for key, db in var.databases :
    key => "jdbc:postgresql://localhost:${db.host_port}/${db.db_name}"
  }
}

output "connection_info" {
  value = {
    for key, db in var.databases :
    key => {
      host_port = db.host_port
      database  = db.db_name
      username  = db.username
    }
  }
}
