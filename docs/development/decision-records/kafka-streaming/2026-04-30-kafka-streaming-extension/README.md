---
status: proposed
date: 2026-04-30
decision-makers: TBD
consulted: TBD
informed: TBD
---

# Kafka Streaming Extension

## Decision

Migrate the Kafka streaming dataplane extension from the standalone PoC repository
(`tractusx-edc-kafka-extension`) into the main `tractusx-edc` codebase. The migration
covers the three core extension modules — including optional Kafka ACL management — and
adds an end-to-end test using Testcontainers Kafka. Demo runtimes and the
selector-strategy bug fix are out of scope and tracked as separate follow-ups.

## Rationale

The Kafka streaming extension currently lives in a separate PoC repo and is therefore
excluded from the Tractus-X release, CI, and quality gates. Catena-X governance
requires connector components to ship from `tractusx-edc` itself, so the extension
must move into the main codebase to be a first-class component of the Tractus-X
connector image.

The PoC also contains demo runtimes (controlplane-local, dataplane-local,
kafka-producer/consumer, docker-compose, Keycloak realm). These are dev scaffolding
and stay in the PoC repo permanently — the migration does not relocate them.

## Approach

### Module placement

Three reusable modules are migrated and co-located under
`edc-extensions/dataplane/kafka/`:

| PoC source                                     | New path in `tractusx-edc`                                              |
|------------------------------------------------|--------------------------------------------------------------------------|
| `poc/kafka-pull/data-address-kafka`            | `edc-extensions/dataplane/kafka/data-address-kafka`                     |
| `poc/kafka-pull/validator-data-address-kafka`  | `edc-extensions/dataplane/kafka/validator-data-address-kafka`           |
| `poc/kafka-pull/kafka-broker-extension`        | `edc-extensions/dataplane/kafka/kafka-broker-extension`                 |

Java packages are renamed to `org.eclipse.tractusx.edc.dataplane.kafka.*`, mirroring
the upstream convention used by `dataplane-proxy-http`. There is no separate
`spi/` module — the data address constants ship inside `data-address-kafka` directly.

### Transfer type and DataAddress

- Transfer type identifier: **`KafkaBroker-PULL`**. On EDC 0.16.0 the data plane derives the transfer
  type as `<sourceType>-PULL`, so it follows the `KafkaBroker` DataAddress type (the PoC's `Kafka-PULL`
  no longer applies).
- DataAddress type: **`KafkaBroker`**.
- The DataAddress carries the Kafka topic, bootstrap servers, security protocol,
  SASL mechanism, group prefix, poll duration, OAuth2 token URL, optional revocation
  URL, client ID, and a vault key reference for the client secret.

### OAuth2 token model

Each transfer mints a fresh OAuth2 access token via the Client Credentials flow.
The token is stored in the provider vault keyed by the transfer process id, and
included in the EDR returned to the consumer. On terminate, the extension calls
the provider's OAuth2 revocation endpoint and removes the token from the vault; suspend
leaves the token untouched. Because the broker validates the JWT by signature and expiry,
immediate broker-level cutoff on suspend/terminate is provided by ACL revocation (see
below), otherwise access ends at the token's TTL.

### Kafka ACL management

Optional broker-level authorization is included, gated by `edc.dataplane.kafka.acl.enabled`
(default `false`). When enabled, on transfer start the extension extracts the `sub` claim from
the minted JWT as the Kafka principal and grants `READ`/`DESCRIBE` on the topic plus `READ` on
the consumer group; on suspend/terminate the ACLs are revoked immediately, closing the access
window independently of token expiry. The ACL admin client is configured via the
`edc.dataplane.kafka.acl.*` settings (`bootstrap.servers`, `security.protocol`, `sasl.mechanism`,
`sasl.jaas.config`) and is covered by `KafkaAclServiceImplTest` and
`KafkaAclServiceImplTestcontainersTest`.

### Runtime wiring

`kafka-broker-extension` is a data-plane extension and is wired into `edc-dataplane-base` as an
`implementation` project dependency (matching the other bundled extensions);
`validator-data-address-kafka` is wired into `edc-controlplane-base` the same way (DataAddress
validation at asset creation). On EDC 0.16.0 the extension registers a `ResourceDefinitionGenerator`
plus a `Provisioner`/`Deprovisioner` (mint and revoke the OAuth token) and an
`EndpointDataReferenceService` that returns the broker EDR and creates/revokes the Kafka ACLs per
activation (start/resume and suspend/terminate). There is no proxied data plane — the
consumer reads directly from the broker using the EDR. (The original control-plane `DataFlowController`
approach was removed when EDC 0.16.0 dropped `DataFlowManager`; see the EDC *inline-data-flow-manager*
decision record.)

### Testing

Unit tests carry over after package rename. A new e2e test module
`edc-tests/e2e/kafka-transfer-tests/` exercises the full flow with a real Kafka
broker (Testcontainers Apache Kafka native image) and a WireMock-backed OAuth2
endpoint. The e2e tests are picked up by the existing CI matrix in
`.github/workflows/verify.yaml` (no workflow edits required).

## Out of scope

The following items are deliberately excluded from this migration and tracked as
separate follow-ups:

1. **Demo runtimes and launchers** — the PoC's `runtimes/edc/*`, `runtimes/kafka/*`,
   `docker-compose.yml`, Keycloak realm, JAAS config, and Bruno collection stay in
   the PoC repo permanently. They are dev scaffolding, not production components.
2. **`DataPlaneSelectorService` selector-strategy bug** — a known PoC issue where
   `selectorService.getAll().getContent().getFirst()` ignores the configured
   `edc.dataplane.client.selector.strategy`. The migrated extension no longer carries any
   custom data-plane selection logic — on EDC 0.16.0 it plugs into the standard data-plane
   framework — so this PoC bug does not apply to the migrated code.
