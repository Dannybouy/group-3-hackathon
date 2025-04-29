# Bank of Anthos

![GitHub branch check runs](https://img.shields.io/github/check-runs/tkdreamdev/group-3-hackathon/main)

[![Website](https://img.shields.io/website?url=https%3A%2F%2Fdreamdev3.live&label=live%20demo)](https://dreamdev3.live)

**Bank of Anthos** is an HTTP-based web app that simulates a bank's payment processing network, allowing users to create artificial bank accounts and complete transactions.

## Added Changes

- Added a new email field to the database schema to store users’ email addresses securely and reliably.
- Implemented a welcome email feature that automatically sends a personalized greeting upon account creation.
- Developed functionality to generate account statements for a specified date range, export them as PDF documents, and automatically email them to users.
- Integrated a credit score evaluation system that allows users to apply for loans based on their proven creditworthiness.
- Enabled a comprehensive dark mode theme throughout the application to improve user experience in low‑light environments.
- Created individual Cloud Build configuration files for each microservice to ensure scalable and efficient continuous integration and deployment.
- Developed a custom cloud monitoring dashboard that provides real-time insights into service performance and health metrics.
- Migrated the database from PostgreSQL to Cloud SQL to enhance reliability, scalability, and managed database services.
- Configured HTTPS across the application to enforce secure communication via TLS encryption and protect user data.

Our application utilizes the following Google Cloud products: [Google Kubernetes Engine (GKE)](https://cloud.google.com/kubernetes-engine/docs), [Cloud SQL](https://cloud.google.com/sql/docs), [Cloud Build](https://cloud.google.com/build/docs), [Cloud Monitoring](https://cloud.google.com/monitoring/docs), [Cloud Logging](https://cloud.google.com/logging/docs).

## Screenshots

| Sign in                                                                                                                                       | Home                                                                                                                                                                     |
| --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| [![Login](/pre-rollout/images_pre_rollout/a_demo_of_my_group_app_login.png)](pre-rollout/images_pre_rollout/a_demo_of_my_group_app_login.png) | [![User Transactions](/pre-rollout/images_pre_rollout/a_demo_of_my_group_app_transactions.png)](/pre-rollout/images_pre_rollout/a_demo_of_my_group_app_transactions.png) |

## Service architecture

![Architecture Diagram](/cloudsql/arch.png)

| Service                                               | Language      | Description                                                                                                                             |
| ----------------------------------------------------- | ------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| [frontend](/src/frontend)                             | Python        | Exposes an HTTP server to serve the website. Contains login page, signup page, and home page.                                           |
| [ledger-writer](/src/ledger/ledgerwriter)             | Java          | Accepts and validates incoming transactions before writing them to the ledger.                                                          |
| [balance-reader](/src/ledger/balancereader)           | Java          | Provides efficient readable cache of user balances, as read from `ledger-db`.                                                           |
| [transaction-history](/src/ledger/transactionhistory) | Java          | Provides efficient readable cache of past transactions, as read from `ledger-db`.                                                       |
| [ledger-db](/src/ledger/ledger-db)                    | PostgreSQL    | Ledger of all transactions. Option to pre-populate with transactions for demo users.                                                    |
| [user-service](/src/accounts/userservice)             | Python        | Manages user accounts and authentication. Signs JWTs used for authentication by other services.                                         |
| [contacts](/src/accounts/contacts)                    | Python        | Stores list of other accounts associated with a user. Used for drop down in "Send Payment" and "Deposit" forms.                         |
| [accounts-db](/src/accounts/accounts-db)              | PostgreSQL    | Database for user accounts and associated data. Option to pre-populate with demo users.                                                 |
| [loadgenerator](/src/loadgenerator)                   | Python/Locust | Continuously sends requests imitating users to the frontend. Periodically creates new accounts and simulates transactions between them. |

## Deployment (GKE)

1. Ensure you have the following requirements:

   - [Google Cloud project](https://cloud.google.com/resource-manager/docs/creating-managing-projects#creating_a_project).
   - Shell environment with `gcloud`, `git`, and `kubectl`.

2. Clone the repository.

   ```sh
   git clone https://github.com/Dannybouy/group-3-hackathon.git
   cd group-3-hackathon/
   ```

3. Set the Google Cloud project and region and ensure the Google Kubernetes Engine API is enabled.

   ```sh
   export PROJECT_ID=<PROJECT_ID>
   export REGION=us-central1
   gcloud services enable container.googleapis.com \
     --project=${PROJECT_ID}
   ```

   Substitute `<PROJECT_ID>` with the ID of your Google Cloud project.

4. Create a GKE cluster and get the credentials for it.

   ```sh
   gcloud container clusters create-auto bank-of-anthos \
     --project=${PROJECT_ID} --region=${REGION}
   ```

   Creating the cluster may take a few minutes.

5. Deploy Bank of Anthos to the cluster.

   ```sh
   kubectl apply -f ./extras/jwt/jwt-secret.yaml
   kubectl apply -f ./kubernetes-manifests
   ```

6. Wait for the pods to be ready.

   ```sh
   kubectl get pods
   ```

   After a few minutes, you should see the Pods in a `Running` state:

   ```
   NAME                                  READY   STATUS    RESTARTS   AGE
   accounts-db-6f589464bc-6r7b7          1/1     Running   0          99s
   balancereader-797bf6d7c5-8xvp6        1/1     Running   0          99s
   contacts-769c4fb556-25pg2             1/1     Running   0          98s
   frontend-7c96b54f6b-zkdbz             1/1     Running   0          98s
   ledger-db-5b78474d4f-p6xcb            1/1     Running   0          98s
   ledgerwriter-84bf44b95d-65mqf         1/1     Running   0          97s
   loadgenerator-559667b6ff-4zsvb        1/1     Running   0          97s
   transactionhistory-5569754896-z94cn   1/1     Running   0          97s
   userservice-78dc876bff-pdhtl          1/1     Running   0          96s
   ```

7. Access the web frontend in a browser using the frontend's external IP.

   ```sh
   kubectl get service frontend | awk '{print $4}'
   ```

   Visit `http://EXTERNAL_IP` in a web browser to access your instance of Bank of Anthos.

8. Once you are done with it, delete the GKE cluster.

   ```sh
   gcloud container clusters delete bank-of-anthos \
     --project=${PROJECT_ID} --region=${REGION}
   ```

   Deleting the cluster may take a few minutes.

## Cloud Build

We leverage Cloud Build triggers to fully automate our CI/CD pipeline for each microservice. Below is what happens on every push to `main` when the corresponding `cloudbuild.yaml` is modified:

1. Preconditions

   - Cloud Build API and Artifact Registry API must be enabled in your project.
   - Service account for Cloud Build needs “Artifact Registry Writer” and “Kubernetes Developer” roles on the cluster.
   - Ensure `$PROJECT_ID`, `$REGION`, and necessary IAM bindings are configured.

2. Trigger behavior

   - Filters on changes to the service’s own `cloudbuild.yaml` path.
   - Builds the Docker image (or Jib/Maven build for Java services).
   - Pushes the image to Artifact Registry under `<region>-docker.pkg.dev/$PROJECT_ID/<repo>/<service>`.
   - Authenticates to the GKE cluster and updates the Deployment or StatefulSet image in the `default` namespace.

3. Naming & conventions

   - Trigger names follow the pattern `<service>-trigger`.
   - Path filters use regex anchored to the `src/<service>/cloudbuild.yaml` location.
   - Artifacts are tagged with the short commit SHA (`$SHORT_SHA`).

4. Viewing & troubleshooting
   - List all triggers:
     ```sh
     gcloud beta builds triggers list --project=$PROJECT_ID
     ```
   - Inspect logs for a build:
     ```sh
     gcloud beta builds log <BUILD_ID> --project=$PROJECT_ID
     ```
   - Manually invoke a trigger:
     ```sh
     gcloud beta builds triggers run <TRIGGER_ID> --branch=main
     ```

| Service            | Trigger name                 | Path filter                         | Build config                                    |
| ------------------ | ---------------------------- | ----------------------------------- | ----------------------------------------------- |
| loadgenerator      | `loadgenerator-trigger`      | `^src/loadgenerator/**`             | `src/loadgenerator/cloudbuild.yaml`             |
| frontend           | `frontend-trigger`           | `^src/frontend/**`                  | `src/frontend/cloudbuild.yaml`                  |
| userservice        | `userservice-trigger`        | `^src/accounts/userservice/**`      | `src/accounts/userservice/cloudbuild.yaml`      |
| contacts           | `contacts-trigger`           | `^src/accounts/contacts/**`         | `src/accounts/contacts/cloudbuild.yaml`         |
| accounts-db        | `accounts-db-trigger`        | `^src/accounts/accounts-db/**`      | `src/accounts/accounts-db/cloudbuild.yaml`      |
| ledgerwriter       | `ledgerwriter-trigger`       | `^src/ledger/ledgerwriter/**`       | `src/ledger/ledgerwriter/cloudbuild.yaml`       |
| balancereader      | `balancereader-trigger`      | `^src/ledger/balancereader/**`      | `src/ledger/balancereader/cloudbuild.yaml`      |
| transactionhistory | `transactionhistory-trigger` | `^src/ledger/transactionhistory/**` | `src/ledger/transactionhistory/cloudbuild.yaml` |
| ledger-db          | `ledger-db-trigger`          | `^src/ledger/ledger-db/**`          | `src/ledger/ledger-db/cloudbuild.yaml`          |

## Cloud SQL

This subsection contains instructions and Kubernetes manifests for overriding the default in-cluster PostgreSQL databases (`accountsdb` + `ledgerdb`) with Google Cloud SQL.

## How it works

The [setup scripts](/src/accounts/contacts) provided will provision a Cloud SQL instance in your Google Cloud Project. The script will then create two databases - one for the **accounts DB**, one for the **ledger DB**. This replaces the two separate PostgreSQL StatefulSets used in Bank of Anthos by default.

## Setup

1. **Create a [Google Cloud project](https://cloud.google.com/resource-manager/docs/creating-managing-projects)** if you don't already have one.

2. **Set environment variables** corresponding to your project, desired GCP region/zone, and the Kubernetes namespace into which you want to deploy Bank of Anthos.

```
export PROJECT_ID="dreamdev-team3"
export DB_REGION="us-east1"
export ZONE="us-east1-b"
export CLUSTER="dreamdev-team3-cluster"
export NAMESPACE="default"
```

3. **Enable the [GKE API](https://cloud.google.com/kubernetes-engine/docs/reference/rest)**. This may take a few minutes.

```
gcloud services enable container.googleapis.com
```

4. **Create a GKE cluster** with [Workload Identity](https://cloud.google.com/kubernetes-engine/docs/how-to/workload-identity#overview) enabled. Workload Identity lets you use a Kubernetes service account like a Google Cloud service account, giving your pods granular Google Cloud API permissions - in this case, permission for the Bank of Anthos Pods to access Cloud SQL.

```
gcloud container clusters create $CLUSTER \
   --project=$PROJECT_ID --zone=$ZONE \
   --machine-type=e2-standard-4 --num-nodes=4 \
   --workload-pool="$PROJECT_ID.svc.id.goog"
```

5. **Run the Workload Identity setup script** for your new cluster. This script creates a Google Service Account (GSA) and Kubernetes Service Account (KSA), associates them together, then grants the service account permission to access Cloud SQL.

```
./setup_workload_identity.sh
```

6. **Run the Cloud SQL instance create script**. This takes a few minutes to complete.

```
./create_cloudsql_instance.sh
```

7. **Create a Cloud SQL admin demo secret** in your GKE cluster. This gives your in-cluster Cloud SQL client a username and password to access Cloud SQL. (Note that admin/admin credentials are for demo use only and should never be used in a production environment.)

```
export INSTANCE_NAME='bank-of-anthos-db'
export INSTANCE_CONNECTION_NAME=$(gcloud sql instances describe $INSTANCE_NAME --format='value(connectionName)')

kubectl create secret -n $NAMESPACE generic cloud-sql-admin \
 --from-literal=username=admin --from-literal=password=admin \
 --from-literal=connectionName=$INSTANCE_CONNECTION_NAME
```

8. **Deploy Bank of Anthos** to your cluster. Each backend Deployment (`userservice`, `contacts`, `transactionhistory`, `balancereader`, and `ledgerwriter`) is configured with a [Cloud SQL Proxy](https://cloud.google.com/sql/docs/mysql/sql-proxy#what_the_proxy_provides) sidecar container. Cloud SQL Proxy provides a secure TLS connection between the backend GKE pods and your Cloud SQL instance.

This command will also deploy two Kubernetes Jobs, to populate the accounts and ledger dbs with Tables and test data.

```
kubectl apply -n $NAMESPACE -f ./kubernetes-manifests/config.yaml
kubectl apply -n $NAMESPACE -f ./populate-jobs
kubectl apply -n $NAMESPACE -f ./kubernetes-manifests
```

9. Wait a few minutes for all the pods to be `RUNNING`. (Except for the two `populate-` Jobs. They should be marked `0/3 - Completed` when they finish successfully.)

```
NAME                                  READY   STATUS      RESTARTS   AGE
balancereader-d48c8d84c-j7ph7         2/2     Running     0          2m56s
contacts-bbfdbb97f-vzxmv              2/2     Running     0          2m55s
frontend-65c78dd78c-tsq26             1/1     Running     0          2m55s
ledgerwriter-774b7bf7b9-jpz7l         2/2     Running     0          2m54s
loadgenerator-f489d8858-q2n46         1/1     Running     0          2m54s
populate-accounts-db-wrh4m            0/3     Completed   0          2m54s
populate-ledger-db-422cr              0/3     Completed   0          2m53s
transactionhistory-747476548c-j2zqx   2/2     Running     0          2m53s
userservice-7f6df69544-nskdf          2/2     Running     0          2m53s
```

10. Access the Bank of Anthos frontend at the frontend service `EXTERNAL_IP`, then log in as `test-user` with the pre-populated credentials added to the Cloud SQL-based `accounts-db`. You should see the pre-populated transaction data show up, from the Cloud SQL-based `ledger-db`. You're done!

## HTTPS Configuration

To secure the frontend service with HTTPS using a custom domain and a Google-managed SSL certificate, follow these steps:

1.  **Reserve a Static External IP Address:**
    Before configuring DNS and SSL, reserve a static IP address in the same region as your GKE cluster. This ensures the IP address assigned to your frontend load balancer doesn't change.

    ```sh
    gcloud compute addresses create frontend-static-ip --global --project=${PROJECT_ID}
    # Note the reserved IP address
    gcloud compute addresses describe frontend-static-ip --global --project=${PROJECT_ID} --format='value(address)'
    ```

2.  **Configure DNS:**
    Go to your domain registrar or DNS provider and create an `A` record pointing your desired domain to the static IP address reserved in step 1. DNS propagation might take some time.

3.  **Create a ManagedCertificate Resource:**
    Create a Kubernetes manifest for a `ManagedCertificate`. This resource tells Google Cloud to provision and manage an SSL certificate for your domain.

    ```yaml
    # managed-certificate.yaml
    apiVersion: networking.gke.io/v1
    kind: ManagedCertificate
    metadata:
      name: frontend-managed-cert
    spec:
      domains:
        - yourdomain # Replace with your domain
    ```

    Apply the manifest: `kubectl apply -f managed-certificate.yaml`
    It can take several minutes for the certificate to be provisioned. Check its status:

    ```sh
    kubectl describe managedcertificate frontend-managed-cert
    ```

    Look for the `CertificateStatus` to become `Active`.

4.  **Create/Update Ingress Resource:**
    Create or update an Ingress resource to manage external access to the frontend service, associating it with the static IP and the managed certificate.

    ```yaml
    # frontend-ingress.yaml
    apiVersion: networking.k8s.io/v1
    kind: Ingress
    metadata:
      name: frontend-ingress
      annotations:
        kubernetes.io/ingress.global-static-ip-name: 'frontend-static-ip' # Name of the reserved static IP
        networking.gke.io/managed-certificates: 'frontend-managed-cert'
        kubernetes.io/ingress.class: 'gce'
    spec:
      rules:
        - host: yourdomain # Replace with your domain
          http:
            paths:
              - path: /*
                pathType: Prefix # Explicitly set path type to Prefix
                backend:
                  service:
                    name: frontend
                    port:
                      number: 80 # Traffic hits the LoadBalancer on port 80, then forwarded to targetPort 8080
    ```

    Apply the manifest: `kubectl apply -f frontend-ingress.yaml`
    Check the Ingress status and associated events:

    ```sh
    kubectl describe ingress frontend-ingress
    ```

    Wait for the Google Cloud Load Balancer to be provisioned and associated with the static IP and certificate. This can take several minutes.

5.  **Verify:**
    Once the `ManagedCertificate` status is `Active` and the `Ingress` resource has successfully provisioned the load balancer (check events using `kubectl describe ingress frontend-ingress`), you should be able to access your application securely via `https://your-domain`. Traffic will be automatically redirected from HTTP to HTTPS.

## Microservice Documentation

- [Frontend Service](/src/frontend/README.md)
- [User Service](/src/accounts/userservice/README.md)
- [Contacts Service](/src/accounts/contacts/README.md)
- [Accounts DB Service](/src/accounts/accounts-db/README.md)
- [Ledger DB Service](/src/ledger/ledger-db/README.md)
- [Ledger Writer Service](/src/ledger/ledgerwriter/README.md)
- [Balance Reader Service](/src/ledger/balancereader/README.md)
- [Transaction History Service](/src/ledger/transactionhistory/README.md)
- [Load Generator Service](/src/loadgenerator/README.md)

## Additional Documentation

- [Development](/docs/development.md) to learn how to run and develop this app locally.
- [Workload Identity](/docs/workload-identity.md) to learn how to set-up Workload Identity.
- [Troubleshooting](/docs/troubleshooting.md) to learn how to resolve common problems.

## Additional reading

- GKE Deployments with Cloud Build: https://cloud.google.com/build/docs/deploying-builds/deploy-gke
- Artifact Registry quickstart: https://cloud.google.com/artifact-registry/docs/docker/quickstart
- Cloud Build triggers overview: https://cloud.google.com/build/docs/automating-builds/create-manage-triggers
