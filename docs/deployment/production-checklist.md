# Production checklist

The chart defaults are chosen so that `helm install` works **out of the box on a laptop**. They are not secure enough
for production. Go through this list before you connect a connector to a live dataspace.

!!! danger "The defaults are for development"

    The bundled Vault runs in **dev mode** with the root token `root`, and the bundled PostgreSQL has **no
    persistence**. Restarting a pod can lose all your agreements and keys.

## Infrastructure

- [ ] **External PostgreSQL.** Set `install.postgresql: false` and point `postgresql.jdbcUrl`,
      `postgresql.auth.username` and `postgresql.auth.password` to a managed, backed-up database. Use a dedicated user
      rather than an admin user (see the chart's
      [database configuration](../../charts/tractusx-connector/README.md#database-configuration)).
- [ ] **External Vault.** Set `install.vault: false` and configure `vault.hashicorp.url` and a token with a narrow
      policy. Avoid HashiCorp Vault **1.18.1**, which has a known path-handling bug.
- [ ] **Persistent identities.** Set `participant.id` (your DID), `participant.bpnl` and `participant.contextId`.
      **Never change `participant.contextId`** after the first start: it is written into the migrated data.

## Secrets

- [ ] **Do not put secrets in `values.yaml`.** The chart renders these as plain environment values:
      the management API key, the consumer proxy API key, the Vault token, the AWS secret key and the database
      password (when you use an external database). Store them in Kubernetes Secrets and inject them with
      `controlplane.envValueFrom` / `dataplane.envValueFrom`, as shown in
      [Configuration → secrets](configuration.md#passing-secrets-safely).
- [ ] **Strong management API credentials.** Replace the default `authKey`, or better, switch to delegated auth by
      setting `controlplane.endpoints.management.jwksUrl` so that the API accepts JWTs from your identity provider.
- [ ] **Vault aliases are filled.** `dcp.sts.oauth.client.secret_alias`, `dataplane.token.signer.privatekey_alias` and
      `dataplane.token.verifier.publickey_alias` must name existing Vault entries.

## Network

- [ ] **Expose only what partners need.** Only the **protocol** (DSP) endpoint of the control plane and the **public**
      endpoint of the data plane must be reachable from the internet. Keep `management`, `control` and `proxy` internal.
- [ ] **TLS on every public ingress.** Set `tls.enabled: true` and either `tls.secretName` or a cert-manager
      `issuer` / `clusterIssuer`. The chart switches the advertised DSP and public URLs to `https://` only when the
      first ingress has TLS enabled.
- [ ] **NetworkPolicy.** Enable `networkPolicy.enabled: true` and restrict `networkPolicy.controlplane.from` /
      `networkPolicy.dataplane.from`. The default `namespaceSelector: {}` allows every namespace.

## Trust

- [ ] **Trusted issuers.** Configure `dcp.trustedIssuers` with the DIDs of the issuers your dataspace trusts.
- [ ] **BDRS.** In Catena-X, set `controlplane.bdrs.server.url` to the BDRS of the dataspace.
- [ ] **Policy validation stays on.** Keep `controlplane.policy.validation.enabled: true` (the default) so that
      malformed Catena-X policies are rejected when they are created.

## Runtime

- [ ] **Log level.** The chart default is `DEBUG`. Set `controlplane.logs.level` / `dataplane.logs.level` to `INFO`.
- [ ] **Resources.** Defaults are requests `500m` / `1024Mi` and limits `1.5` CPU / `1024Mi`. Size them from your load
      tests.
- [ ] **Read-only root filesystem.** It is enabled by default and `/tmp` is writable. If you use multipart uploads and
      see write errors, check the temp directory before you relax this setting.
- [ ] **Autoscaling.** The HPA templates use `autoscaling/v2beta1`, which current Kubernetes versions no longer serve.
      Check this before you set `autoscaling.enabled: true`; scale with `replicaCount` instead. More than one replica
      also turns off DID self-deregistration.
- [ ] **Upgrades rehearsed.** Read [Upgrades](upgrades.md) and run the upgrade on a staging copy of your database.

## Verify

```bash
helm test <release>                    # runs the chart's built-in test
kubectl get pods -l app.kubernetes.io/instance=<release>
curl -s https://<public-host>/api/v1/dsp/.well-known/dspace-version   # DSP version discovery
```

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
