# trade-surveillance
A real-time trade surveillance platform

trade-surveillance/
  pom.xml                 # parent POM
  docker-compose.yml      # local infrastructure
  infra/keycloak/         # realm export
  libs/trade-common/      # shared event classes
  services/
    ingestion-service/
    detection-service/
    alert-case-service/
    gateway/
  ui/trade-ui/            # React app (Week 5)
  deploy/                 # Kubernetes files (Week 7)
  docs/adr/               # decision records
  README.md  LEARNING.md