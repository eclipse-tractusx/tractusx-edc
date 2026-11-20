# Observability

## Health checks

Each runtime exposes liveness and readiness on its **default** API context (port 8080 in the chart):

| Probe | URL |
|---|---|
| Liveness | `GET http://<pod>:8080/api/check/liveness` |
| Readiness | `GET http://<pod>:8080/api/check/readiness` |

The chart configures both as Kubernetes probes (initial delay 30 s, period 10 s, timeout 5 s, failure threshold 6).
Tune them under `controlplane.livenessProbe` / `readinessProbe` and the `dataplane.*` equivalents.

## Logs

The runtimes log through Log4j 2. By default the chart writes **JSON logs** to stdout, ready for Loki, Elasticsearch or
any other log pipeline.

| Helm value | Default | Meaning |
|---|---|---|
| `log4j2.enableJsonLogs` | `true` | Use the JSON layout from `log4j2.config` (mounted as `/app/log4j2.yaml`) |
| `log4j2.config` | JSON template layout | Full Log4j 2 YAML configuration, if you want your own |
| `controlplane.logs.level`, `dataplane.logs.level` | `DEBUG` | Log level passed as `--log-level` |

!!! tip "Use INFO in production"

    `DEBUG` is verbose and can make it hard to find what matters. Switch to `INFO` and raise the level temporarily when
    you investigate a problem.

What to search for:

| You want to know… | Look for |
|---|---|
| Whether the runtime started | the line announcing that the runtime is ready, after all extensions were started |
| Why a catalog or negotiation was rejected | `Policy`, `evaluation`, `credential`, `VerifiablePresentation` |
| Database migration problems | `Flyway`, `migration` |
| Token refresh problems | `refresh`, `token` |

## OpenTelemetry

The image ships the **OpenTelemetry Java agent**. It is **off by default**. Enable and configure it through the
properties file the chart mounts as `/app/opentelemetry.properties`:

```yaml
controlplane:
  opentelemetry: |-
    otel.javaagent.enabled=true
    otel.service.name=my-connector-controlplane
    otel.exporter.otlp.endpoint=http://otel-collector:4318
```

The same exists under `dataplane.opentelemetry`. All `otel.*` keys follow the
[agent configuration](https://opentelemetry.io/docs/zero-code/java/agent/configuration/).

## Business events

The `event-subscriber` extension sends **connector events** (negotiation, transfer, …) as OpenTelemetry log records to
a collector, which gives you an audit trail of data exchanges.

| Setting | Default | Meaning |
|---|---|---|
| `tx.edc.otel.events.subscriber.active` | `false` | Turn the subscriber on |
| `tx.edc.otel.events.endpoint` | `http://localhost:4318/v1/logs` | OTLP/HTTP logs endpoint |
| `tx.edc.otel.service.name` | `unknown_service` | `service.name` of the records |

Set them with `controlplane.env` / `dataplane.env`, for example `TX_EDC_OTEL_EVENTS_SUBSCRIBER_ACTIVE: "true"`.

Your applications can also be notified directly: register `callbackAddresses` on negotiations and transfers, or
configure static callbacks. See the [EDR walkthrough](../usage/management-api-walkthrough/07_edrs.md).

## Useful Management API queries

```bash
Q='{"@context":{"@vocab":"https://w3id.org/edc/v0.0.1/ns/"},"@type":"QuerySpec","limit":100}'

# Negotiations that ended badly, with the reason
curl -s -X POST "$MGMT/v3/contractnegotiations/request" -H "X-Api-Key: $KEY" -H "Content-Type: application/json" \
  -d "$Q" | jq '.[] | select(.state=="TERMINATED") | {"@id", counterPartyId, errorDetail}'

# Running transfers
curl -s -X POST "$MGMT/v3/transferprocesses/request" -H "X-Api-Key: $KEY" -H "Content-Type: application/json" \
  -d "$Q" | jq '.[] | select(.state=="STARTED") | {"@id", assetId, contractId}'
```

A terminated negotiation or transfer carries an `errorDetail` field that explains why.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
