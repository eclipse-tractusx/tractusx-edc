# APIs

## Interactive API documentation

The OpenAPI specifications of each release are published as Swagger UI. They merge the upstream EDC APIs with the
Tractus-X additions.

| Runtime | Swagger UI |
|---|---|
| Control plane | [openapi/control-plane-api](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/) |
| Data plane | [openapi/data-plane-api](https://eclipse-tractusx.github.io/tractusx-edc/openapi/data-plane-api/) |

Specifications of a specific release live under a version folder, for example
`https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/0.13.0/control-plane.yaml`.
To generate them locally run `./gradlew resolve`; the YAML files appear under `resources/openapi/yaml/`.

## Ports and paths in the Helm chart

=== "Control plane"

    | API context | Port | Path | Who calls it | Expose? |
    |---|---|---|---|---|
    | default | 8080 | `/api` | Kubernetes probes | no |
    | management | 8081 | `/management` | Your applications | internal only |
    | control | 8083 | `/control` | Data plane | no |
    | protocol | 8084 | `/api/v1/dsp` | Other connectors (DSP) | **yes**, via ingress |
    | metrics | 9090 | `/metrics` | Monitoring | no |

=== "Data plane"

    | API context | Port | Path | Who calls it | Expose? |
    |---|---|---|---|---|
    | default | 8080 | `/api` | Kubernetes probes | no |
    | public | 8081 | `/api/public` | Consumers fetching data, token refresh | **yes**, via ingress |
    | control | 8084 | `/api/control` | Control plane | no |
    | proxy | 8186 | `/proxy` | Your applications (consumer proxy) | internal only |
    | metrics | 9090 | `/metrics` | Monitoring | no |

The DSP protocol endpoint has a sub-path per protocol version: DSP 2025-1 under `/api/v1/dsp/2025-1`, DSP 0.8 at
`/api/v1/dsp`. Partners can discover the supported versions at `/api/v1/dsp/.well-known/dspace-version`.

## Management API

The Management API (control plane, `/management`) is the upstream EDC API plus a few Tractus-X endpoints. Authenticate
with the `X-Api-Key` header, or with a JWT when delegated authentication (`jwksUrl`) is configured.

| Upstream resource | Path |
|---|---|
| Assets | `/v3/assets` |
| Policy definitions | `/v3/policydefinitions` |
| Contract definitions | `/v3/contractdefinitions` |
| Catalog | `/v3/catalog/request`, `/v3/catalog/dataset/request` |
| Contract negotiations | `/v3/contractnegotiations` |
| Contract agreements | `/v3/contractagreements` |
| Transfer processes | `/v3/transferprocesses` |

The [Management API walkthrough](../usage/management-api-walkthrough/README.md) explains each of them with examples.

## Endpoints added by Tractus-X

| Method | Path | Plane / context | Purpose |
|---|---|---|---|
| `POST` | `/v3/edrs` | CP management | Negotiate and start a transfer in one call |
| `POST` | `/v3/edrs/request` | CP management | Query cached EDRs |
| `GET` | `/v3/edrs/{transferProcessId}/dataaddress?auto_refresh=true` | CP management | Get a (refreshed) EDR |
| `POST` | `/v3/edrs/{transferProcessId}/refresh` | CP management | Force a token refresh |
| `DELETE` | `/v3/edrs/{transferProcessId}` | CP management | Remove a cached EDR |
| `POST`, `PUT` | `/v3/business-partner-groups` | CP management | Assign a BPN to groups |
| `GET` | `/v3/business-partner-groups/{bpn}`, `/group/{group}`, `/groups` | CP management | Read groups |
| `DELETE` | `/v3/business-partner-groups/{bpn}` | CP management | Remove a BPN |
| `POST` | `/v3/contractagreements/retirements` | CP management | Retire an agreement early |
| `POST` | `/v3/contractagreements/retirements/request` | CP management | List retired agreements |
| `DELETE` | `/v3/contractagreements/retirements/{agreementId}` | CP management | Reactivate an agreement |
| `POST` | `/v3/connectordiscovery/dspversionparams` | CP management | Find the DSP version and address of a connector |
| `POST` | `/v3/connectordiscovery/connectors` | CP management | Find the connectors of a participant |
| `POST` | `/v3/dataflows/{id}/trigger` | DP management | Re-trigger a non-finite push data flow |
| `POST` | `/aas/request` | DP proxy (`/proxy`) | Fetch data through the consumer proxy |
| `POST` | `/token` | DP public | Refresh an access token |
| any | `/{path}` | DP public | Provider data access with an EDR token |

!!! warning "Deprecated `v4alpha` paths"

    `/v4alpha/connectordiscovery/*` and `/v4alpha/dataflows/{id}/trigger` are deprecated since 0.13.0. Use the `v3`
    paths above.

To list the endpoints of your checkout, run the Sensei's inventory script:

```bash
.claude/skills/management-api-endpoint/scripts/api-inventory.sh
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
