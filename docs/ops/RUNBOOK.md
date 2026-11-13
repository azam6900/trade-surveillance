# Runbook: recreate the GKE demo environment

Written from the first successful deployment (2026-11-13). Follow the steps in order. The whole sequence takes about one hour, most of it waiting for the cluster and the pipeline.

The GKE cluster bills by the hour while it exists. Recreate it only when you need it (a demo, an interview, a recording), and delete it afterwards (see the last section).

## What stays and what is deleted

| Kept after teardown (costs nothing or almost nothing) | Deleted at teardown (this is what you recreate) |
| --- | --- |
| Google Cloud project, billing link, enabled APIs | The GKE Autopilot cluster `trade` (all pods, the PostgreSQL and MongoDB data) |
| Secret Manager secrets `trade-postgres-password` and `trade-keycloak-admin-password`, and the IAM bindings that let the operator read them | The Artifact Registry repository `trade` (all images; the pipeline rebuilds them) |
| Deployer service account `github-deployer` and its two roles | Any `kubectl port-forward` terminals |
| Workload Identity Federation pool `github`, provider `github-oidc`, and its binding | |
| Project-level `artifactregistry.reader` role for the cluster nodes | |
| GitHub environment `production` and `.github/workflows/cd.yml` | |
| The Git repository (`deploy/`, `infra/`, the Helm chart) | |

## Before you start

Use Git Bash, in the repository root. Set the values every command uses (they are identifiers, not secrets). If you open a new terminal, run these again:

```bash
export PROJECT_ID=trade-surv-20722
export PROJECT_NUMBER=687409191587
export REGION=northamerica-northeast2
gcloud config set project $PROJECT_ID
```

Check that the kept pieces still exist:

```bash
gcloud auth list                                                       # you are signed in
gcloud billing projects describe $PROJECT_ID --format="value(billingEnabled)"   # True
gcloud secrets list                                                    # both trade-* secrets are listed
gcloud iam workload-identity-pools providers describe github-oidc --location=global \
  --workload-identity-pool=github --format="value(attributeCondition)" # shows your repository, not a placeholder
```

Also make sure nothing else is using the ports for the port-forwards (5173, 8080, 8180): stop `docker compose`, `npm run dev`, and old `kubectl port-forward` windows.

## 1. Create the image repository

(Guide Step 7.7, item 2)

```bash
gcloud artifacts repositories create trade --repository-format=docker --location=$REGION
gcloud artifacts repositories list
```

Check: the list shows `trade` in `northamerica-northeast2`.

## 2. Create the cluster

(Guide Step 7.8)

```bash
gcloud components install gke-gcloud-auth-plugin        # only if it is not installed yet
gcloud container clusters create-auto trade --region=$REGION     # about 10 minutes
gcloud container clusters get-credentials trade --region=$REGION
kubectl config current-context                          # must be gke_trade-surv-20722_northamerica-northeast2_trade
kubectl create namespace trade
kubectl config set-context --current --namespace=trade
```

Check: `kubectl get ns` shows `trade`. **Always check the context before any `kubectl delete` or `helm uninstall`**, because your laptop also has a local cluster.

## 3. Install External Secrets and apply the secret definitions

(Guide Step 7.9, items 2, 4, and 5. The two passwords and the IAM bindings still exist, so items 1 and 3 are skipped.)

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm repo update
helm install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace --wait

kubectl apply -f deploy/secrets/
kubectl get clustersecretstore                          # gcp: STATUS Valid, READY True
kubectl get externalsecret trade-secrets                # STATUS SecretSynced, READY True
kubectl get secret trade-secrets                        # DATA 2
```

`kubectl apply -f deploy/secrets/` must apply **both** files: the `ClusterSecretStore` (`cluster-secret-store.yaml`) and the `ExternalSecret` (`trade-secrets.yaml`). If only the second exists, the sync fails with `ClusterSecretStore "gcp" not found`.

If the first sync reports `PermissionDenied`, wait one or two minutes (Google needs a moment to apply permissions) and force a retry:

```bash
kubectl annotate externalsecret trade-secrets force-sync=$(date +%s) --overwrite
```

## 4. Create the Keycloak realm ConfigMap and the infrastructure

(Guide Step 7.11, item 1)

```bash
kubectl create configmap keycloak-realm --from-file=realm-export.json=infra/keycloak/realm-export.json
kubectl apply -f deploy/infra/
kubectl get pods -w          # Autopilot may need a few minutes to add capacity; Ctrl+C when done
```

Check: five pods (`kafka`, `keycloak`, `mongo`, `postgres`, `redis`), all `1/1 Running`. The ConfigMap must exist **before** Keycloak starts; if you created it afterwards, run `kubectl rollout restart deployment/keycloak`.

## 5. Run the deployment pipeline

On GitHub, open **Actions**, choose **cd**, click **Run workflow**, and **approve the `production` deployment** when asked. The run takes 10 to 15 minutes: it builds five images, pushes them to the new registry, and deploys the Helm chart.

While it runs, watch the cluster:

```bash
kubectl get pods -w
```

Check: the run is green, `kubectl get pods` shows ten pods (`5` infrastructure and `5` applications) all `1/1 Running`, and `helm history trade` shows one `deployed` revision.

## 6. Open the application

Each command in its own terminal (they stop when the terminal closes):

```bash
kubectl port-forward svc/trade-ui 5173:8080
kubectl port-forward svc/gateway 8080:8080
kubectl port-forward svc/keycloak 8180:8080
```

## 7. Verify

```bash
./scripts/smoke.sh               # ends with: All services ready.
./scripts/demo-backend.sh        # eight steps, ending with three audit lines
```

Then open `http://localhost:5173`, log in as `analyst1` / `analyst123`, and confirm the alerts page loads.

## If something fails

| Symptom | Fix |
| --- | --- |
| `ImagePullBackOff` on a `trade/...` image | Give the cluster nodes read access to the registry: `gcloud projects add-iam-policy-binding $PROJECT_ID --member=serviceAccount:$PROJECT_NUMBER-compute@developer.gserviceaccount.com --role=roles/artifactregistry.reader` |
| `ClusterSecretStore "gcp" not found`, or `SecretSyncedError` | `kubectl get clustersecretstore`; if missing, `kubectl apply -f deploy/secrets/`. Read the reason with `kubectl logs -n external-secrets deploy/external-secrets --tail=30` |
| The pipeline fails at the Google login with `Gaia id not found` | `cd.yml` uses the project **number** where the **ID** belongs. The service account email must be `github-deployer@trade-surv-20722.iam.gserviceaccount.com` |
| `rejected by the attribute condition` or `getAccessToken denied` | The repository name in the provider condition or in the `workloadIdentityUser` binding does not exactly match `azam6900/trade-surveillance` (case matters) |
| `denied ... uploadArtifacts` | The repository `trade` was not created (step 1), or `REG` in `cd.yml` has the wrong region or project ID |
| Pods `Pending` for several minutes | Autopilot is adding capacity; wait. `kubectl describe pod <name>` shows the scheduling events |
| Helm reports `timed out waiting for the condition` and rolls back | The pods never became ready: `kubectl get pods`, then `kubectl logs <pod>` and `kubectl describe pod <pod>` |
| A pod restarts again and again | `kubectl logs <pod> --previous`; the first `Caused by:` line names the cause (a wrong password in the secret is the usual one) |
| `kubectl` talks to the wrong cluster | `kubectl config current-context`; switch with `kubectl config use-context <name>` |

## Tear down (when you are finished)

Check the context first. These are the only commands that cost money to leave undone:

```bash
kubectl config current-context        # must be the gke_... context
helm -n trade uninstall trade
gcloud container clusters delete trade --region=$REGION --quiet
gcloud artifacts repositories delete trade --location=$REGION --quiet
```

Check: `gcloud container clusters list` prints nothing. The next day, open Billing, Reports, and filter by this project to confirm there is no ongoing daily charge.