# ADR 0006: Helm on kind and GKE, secrets from Secret Manager, keyless CI/CD

## Decision
- One Helm chart for all applications; the same chart deploys to kind and GKE,
  with only the image registry and tag changing.
- Secrets live in GCP Secret Manager. The External Secrets Operator copies them
  into a Kubernetes Secret, authenticating with Workload Identity (no key files).
- GitHub Actions authenticates to GCP with Workload Identity Federation,
  restricted to this repository; deploys need a manual approval and use
  helm --atomic, so a failed rollout reverts itself.
- Infrastructure in the cluster is demo grade (one pod each, temporary disk).
  Production would use Cloud SQL, Memorystore, MongoDB Atlas, a managed Kafka
  service, and Keycloak with its own database.
- The demo uses kubectl port-forward so browser URLs, the Keycloak redirect, and
  the token issuer stay on localhost.

## Consequences
+ No long-lived credential exists anywhere; nothing secret is in Git.
+ Every deployment is traceable to a commit SHA and can be rolled back.
- Demo data is lost whenever an infrastructure pod restarts.
- Port forwarding is for demos only; real users need an Ingress with HTTPS.
19. Add five lines to LEARNING.md, commit, tag week-7, and push.