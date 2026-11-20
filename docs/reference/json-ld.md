# JSON-LD

The Management API and the Dataspace Protocol speak JSON-LD. A term means what the `@context` expands it to, so
knowing the namespaces saves a lot of debugging.

## Namespaces

| Prefix | IRI | Used for |
|---|---|---|
| `edc` | `https://w3id.org/edc/v0.0.1/ns/` | EDC Management API properties (`@vocab` in most examples) |
| `tx` | `https://w3id.org/tractusx/v0.0.1/ns/` | Tractus-X properties, for example `tx:groups` |
| `tx-auth` | `https://w3id.org/tractusx/auth/` | EDR refresh properties (`refreshToken`, `refreshEndpoint`, …) |
| `cx-policy` | `https://w3id.org/catenax/2025/9/policy/` | Catena-X policy constraints |
| `cx-policy` (legacy) | `https://w3id.org/catenax/policy/` | Deprecated policy namespace, still evaluated |
| `odrl` | `http://www.w3.org/ns/odrl/2/` | Policies |
| `dspace` | `https://w3id.org/dspace/v0.8/` | DSP 0.8 messages |

## Contexts

Use these in your `@context`:

| Context URL | Defines |
|---|---|
| `https://w3id.org/dspace/2025/1/odrl-profile.jsonld` | ODRL terms for DSP 2025-1 |
| `https://w3id.org/catenax/2025/9/policy/context.jsonld` | Catena-X constraint names (`FrameworkAgreement`, `Membership`, `BusinessPartnerGroup`, …) |
| `https://w3id.org/tractusx/auth/v1.0.0` | `tx-auth` refresh terms |
| `https://w3id.org/edc/v0.0.1` | EDC terms |

!!! note "There is no `tx` context document"

    `tx` is only a namespace. Declare it yourself, for example
    `"@context": {"tx": "https://w3id.org/tractusx/v0.0.1/ns/"}`.

A typical access policy for DSP 2025-1:

```json
{
  "@context": [
    "https://w3id.org/dspace/2025/1/odrl-profile.jsonld",
    "https://w3id.org/catenax/2025/9/policy/context.jsonld",
    { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" }
  ],
  "@type": "PolicyDefinition",
  "@id": "membership-policy",
  "policy": {
    "@type": "Set",
    "permission": [{
      "action": "access",
      "constraint": [{
        "leftOperand": "Membership",
        "operator": "eq",
        "rightOperand": "active"
      }]
    }]
  }
}
```

More examples are in the [policy walkthrough](../usage/management-api-walkthrough/02_policies.md).

## Offline contexts

The connector does not download these documents at runtime; it ships them and resolves them from the classpath:

- `https://www.w3.org/2018/credentials/v1`
- `https://w3id.org/security/suites/jws-2020/v1`
- `https://w3id.org/security/suites/ed25519-2020/v1`
- `https://w3id.org/tractusx/auth/v1.0.0`
- `https://w3id.org/edc/v0.0.1`
- `https://w3id.org/catenax/2025/9/policy/context.jsonld`
- `https://w3id.org/catenax/2025/9/policy/odrl.jsonld`

!!! warning "Deprecated contexts"

    `https://w3id.org/tractusx/policy/v1.0.0` (since 0.11.0) and
    `https://w3id.org/catenax/2025/9/policy/odrl.jsonld` (since 0.12.0) are deprecated. Use
    `https://w3id.org/catenax/2025/9/policy/context.jsonld`.

## Debugging tips

- **A typo fails quietly.** With `@vocab` set to the EDC namespace, a typo such as `BusinesPartnerGroup` silently
  becomes an EDC IRI, and the policy validation rejects the unknown left operand.
- **Expand to see the truth.** The [JSON-LD playground](https://json-ld.org/playground/) shows the full IRIs of a
  payload.
- **Prefixes may change.** The Management API returns compacted JSON-LD; the prefixes in a response may differ from
  the ones you sent.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
