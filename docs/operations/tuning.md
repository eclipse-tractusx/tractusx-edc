# Tune for production

The chart defaults are made to start anywhere, not to carry production load. This page lists the knobs that matter
once the connector runs for real, in the order you will usually need them. Change one thing at a time and watch the
effect in your [metrics and logs](observability.md).

Before you tune, work through the [production checklist](../deployment/production-checklist.md): it covers security
and availability; this page covers performance and resource use.

## Settings in the Helm chart

Every EDC setting below can be set as an environment variable through `controlplane.env` or `dataplane.env`: write
the key in upper case with `_` instead of `.` and `-` (`edc.transfer.state-machine.batch-size` becomes
`EDC_TRANSFER_STATE_MACHINE_BATCH_SIZE`). See [Configuration](../deployment/configuration.md).

```yaml title="values.yaml (excerpt)"
controlplane:
  logs:
    level: INFO
  resources:
    requests: { cpu: "1", memory: 2Gi }
    limits: { cpu: "2", memory: 2Gi }
  env:
    JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=75.0"
    EDC_TRANSFER_STATE_MACHINE_BATCH_SIZE: "50"
```

## 1. Log level

The chart sets the log level of both planes to **`DEBUG`** (`controlplane.logs.level`, `dataplane.logs.level`). In
production that is a lot of output, and it may contain request details. Set **`INFO`**, and switch to `DEBUG` only
while you investigate a problem.

## 2. Memory and the JVM

| Value | Chart default |
|---|---|
| `*.resources.requests.memory` / `*.resources.limits.memory` | `1024Mi` / `1024Mi` |
| `*.resources.requests.cpu` / `*.resources.limits.cpu` | `500m` / `1.5` |

The images run Java 25 (Eclipse Temurin). Inside a container the JVM uses **25 % of the memory limit** as maximum
heap by default, so a 1 GiB pod gets about 256 MiB of heap. Give the heap more of the pod with
`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0` in `*.env`, and keep the memory request equal to the limit, so the pod
is not evicted under node pressure.

!!! warning "`debug.enabled` uses `JAVA_TOOL_OPTIONS` too"

    With `controlplane.debug.enabled` or `dataplane.debug.enabled`, the chart sets `JAVA_TOOL_OPTIONS` itself for the
    remote debugger. Do not set it in `env` at the same time; debugging is not for production anyway.

Signs that memory is too small: restarts with `OOMKilled`, long garbage collection pauses in the JVM metrics, or
timeouts under load. CPU limits mainly slow down the start (class loading, JIT); readiness probes start after 30
seconds by default.

## 3. Replicas

Raise `controlplane.replicaCount` and `dataplane.replicaCount` for availability and throughput. Both planes keep
their state in PostgreSQL, so replicas share the work. With more than one control plane replica the chart disables
the DID service self-deregistration, so that one pod shutting down does not remove the connector from the DID
document.

Do **not** use `autoscaling.enabled`: its templates use the `autoscaling/v2beta1` API, which current Kubernetes no
longer serves. Scale with `replicaCount`. See [Resources and scaling](../deployment/helm-chart.md#resources-and-scaling).

## 4. State machines (control plane)

Contract negotiations and transfer processes are driven by state machines that pick up a **batch** of entities from
the database, process them, and **wait** before the next round.

| Setting (prefix + suffix) | Default | Effect |
|---|---|---|
| `<prefix>.state-machine.batch-size` | `20` | Entities processed per round. Higher means more throughput and more database load. |
| `<prefix>.state-machine.iteration-wait-millis` | `1000` | Pause between rounds when there is nothing to do. Lower means faster reactions and more polling. |
| `<prefix>.send.retry.limit` | upstream default | How often a failed message to the partner is retried. |
| `<prefix>.send.retry.base-delay.ms` | upstream default | Base of the exponential backoff between retries. |

The prefixes are `edc.negotiation.consumer` and `edc.negotiation.provider` for contract negotiations, and
`edc.transfer` for transfer processes. Raise the batch size when negotiations or transfers queue up (many entities
in `REQUESTING`, `STARTING` or similar states for a long time), and only together with database capacity.

## 5. Database connections

Each plane uses a connection pool per data source (`default` in the chart):

| Setting | Effect |
|---|---|
| `edc.datasource.default.pool.connections.max-total` | Upper limit of open connections per pod |
| `edc.datasource.default.pool.connections.max-idle` | Connections kept open when idle |
| `edc.datasource.default.pool.connections.min-idle` | Connections opened in advance |
| `edc.sql.fetch.size` | Rows fetched per round trip in queries |

Keep **max-total × replicas of both planes** below the `max_connections` of your PostgreSQL, with room for
migrations and your own tools. A bigger state machine batch needs more connections.

## 6. Caches and timeouts

| Setting | Chart value | Effect |
|---|---|---|
| `dcp.cache.enabled`, `dcp.cache.validity` | `true`, `86400` s | Caches partners' verifiable presentations, saving a call to their wallet on every request. Shorter means revoked credentials are noticed sooner. |
| `controlplane.bdrs.cache_validity_seconds` | `600` | How long the BPN ↔ DID directory is cached (Catena-X). |
| `edc.http.client.timeout.connect`, `edc.http.client.timeout.read` | upstream default | Timeouts of outgoing HTTP calls (partners, wallet, BDRS). Raise when partners are slow, rather than retrying more. |
| `edc.dataplane.transfer.threads` | upstream default | Threads of the data plane for push transfers. |
| `tx.edc.dataplane.token.expiry` | `300` s | Lifetime of access tokens for pull transfers; longer means fewer refreshes. |

## 7. Measure again

- Follow negotiations and transfers per state with the [Management API queries](observability.md#useful-management-api-queries).
- Export JVM and HTTP metrics through [OpenTelemetry](observability.md#opentelemetry).
- Run a load test in a test environment with your real policies: policy evaluation and credential checks dominate
  the cost of a negotiation.

Like any open-source software, Tractus-X EDC comes without warranty: you are responsible for sizing and operating
it. Share what you learn in the [Connector Open Meeting](https://eclipse-tractusx.github.io/community/open-meetings#edc-weekly).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
