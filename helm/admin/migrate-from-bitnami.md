# Migrating from Bitnami Helm Charts

## PostgreSQL

- [ ] `kc edit secret metacatbrooke-metacat-secrets`, make a note of the PostgreSQL password, then remove the `POSTGRES_PASSWORD:` entry
- [ ] Edit `helm/admin/cloudnative-pg-secret.yaml` to insert the postgres **username and password**, then `kubectl apply -f` it. DO NOT COMMIT TO GH! Instead, GPG-encrypt and add to security repo, then delete local copy.
- [ ] edit values.yaml:
    ```yaml
    # ADD:
    postgresql:
      persistence:
        size: <set volume size here>

    # REMOVE:
      auth:
        existingSecret: <release-name>-metacat-cnpg
    ```
