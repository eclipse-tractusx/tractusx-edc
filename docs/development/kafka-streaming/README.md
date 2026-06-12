# Kafka Streaming Extension

The Kafka streaming extension provides the `KafkaBroker-PULL` transfer type, enabling real-time,
event-driven data exchange between sovereign partners over Apache Kafka. It is implemented as an EDC
data-plane extension and ships in the standard `tractusx-connector` runtime (via `edc-dataplane-base`),
so it does not require a separate deployment.

Unlike a proxied transfer, data flows **directly** from the provider's Kafka broker to the consumer.
The EDC stays in charge of access: the data-plane extension provisions short-lived OAuth2
credentials (and, optionally, Kafka ACLs) for the duration of a negotiated transfer, so the provider
retains full control over the consumer's access throughout.

<!-- TOC -->
* [Overview](#overview)
* [Architecture](#architecture)
  * [Components](#components)
  * [Component diagram](#component-diagram)
* [Transfer Workflow](#transfer-workflow)
  * [A. Provisioning/Deprovisioning](#a-provisioningdeprovisioning)
  * [B. Initiating the Transfer](#b-initiating-the-transfer)
  * [C. Data Streaming](#c-data-streaming)
  * [D. Suspending/Terminating](#d-suspendingterminating)
* [DataAddress Schema](#dataaddress-schema)
* [Configuration](#configuration)
  * [EDC data plane properties](#edc-data-plane-properties)
  * [Kafka broker](#kafka-broker)
  * [Keycloak](#keycloak)
* [Security and Token Model](#security-and-token-model)
  * [Authentication flow](#authentication-flow)
  * [Token expiry and revocation](#token-expiry-and-revocation)
  * [Kafka ACL management (optional)](#kafka-acl-management-optional)
  * [Transport encryption (SASL_SSL)](#transport-encryption-sasl_ssl)
* [Interoperability and Standards](#interoperability-and-standards)
* [Troubleshooting](#troubleshooting)
* [Decision records](#decision-records)
* [NOTICE](#notice)
<!-- TOC -->

## Overview

Kafka topics are treated as EDC data assets: a topic carries domain-specific business data (e.g.
semantic models such as `SerialPart` or `Batch`), the provider registers it as an asset with a usage
policy, and consumers gain access through the standard EDC contract negotiation. On successful
negotiation the consumer receives an Endpoint Data Reference (EDR) containing the connection details
and an OAuth2 token, which it uses to subscribe to and poll the topic directly. All exchanges are
tracked in the EDC (contract id, asset id, timestamps) for auditability.

`SASL/OAUTHBEARER` ([RFC 7628](https://datatracker.ietf.org/doc/html/rfc7628)) is used as the Kafka
authentication mechanism, allowing OAuth2 tokens to authenticate the consumer against the broker.

Typical Catena-X use cases that benefit from Kafka streaming include Quality Management (predictive
maintenance, early-warning notifications), Digital Twin / Asset Administration Shell (real-time
operational and condition monitoring), Demand & Capacity Management, Traceability, Circular Economy /
Product Pass, and ESG monitoring.

## Architecture

### Components

The extension consists of three modules under `edc-extensions/dataplane/kafka/`:

1. **Kafka Broker Extension** (`kafka-broker-extension`): a data-plane extension that adds the
   `KafkaBroker-PULL` transfer type. It provisions OAuth2 credentials and optional Kafka ACLs, stores
   tokens in the EDC Vault, builds the EDR for the consumer, and revokes credentials on
   suspend/terminate.
2. **Data Address Kafka** (`data-address-kafka`): defines the `KafkaBroker` data address format
   (topic, bootstrap servers, security protocol, SASL mechanism, OAuth2 settings, consumer group).
3. **Validator Data Address Kafka** (`validator-data-address-kafka`): validates that a Kafka data
   address contains all required properties with valid values.

These integrate with:

- **Eclipse Dataspace Connector (EDC)** — the core framework for negotiation and data exchange
- **Apache Kafka** — the messaging platform for data streaming
- **OAuth2 provider** (e.g. Keycloak) — for authentication and authorization

The roles involved at runtime are the **Control Plane** (negotiation, policy checks), the **Kafka
Extension** (credential orchestration and Vault access), the **Kafka Service** (broker enforcing
SASL/OAUTHBEARER and topic authorization), the **OAuth Service** (issues and revokes tokens), and the
**Vault** (secure storage of temporary credentials). On the consumer side, the **Consumer Control
Plane** receives the EDR and the **Consumer Application** uses it to instantiate a Kafka consumer.

### Component diagram

![Component diagram EDC Kafka Extension](diagrams/Component%20diagram%20EDC%20Kafka%20Extension.png)

## Transfer Workflow

The transfer is defined by four phases, each illustrated by a sequence diagram.

### A. Provisioning/Deprovisioning

**Purpose:** securely create or delete consumer credentials.

1. **Provisioning:** the Consumer Control Plane sends a `TransferRequestMessage` to the Provider
   Control Plane, which sends a `DataFlowStartMessage` (Data Plane Signaling) to the data plane.
   There the Kafka Extension reads the OAuth2 client secret from the Vault, obtains a short-lived
   access token from the OAuth Service, stores it in the Vault, and returns the provisioned Kafka
   `DataAddress` in the `DataFlowResponseMessage`.
2. **Deprovisioning:** when the transfer is terminated, the Provider Control Plane signals the
   termination to the data plane; the Kafka Extension revokes the token via the OAuth Service and
   deletes it from the Vault.

![Sequence diagram EDC Kafka Extension provisioning-deprovisioning](diagrams/Sequence%20diagram%20EDC%20Kafka%20Extension%20provisioning-deprovisioning.png)

### B. Initiating the Transfer

**Purpose:** start a transfer process with dynamic credentials and EDR creation.

1. The Consumer Control Plane instructs the Provider Control Plane to start the transfer; the Provider
   Control Plane performs policy and contract verification.
2. The Provider Control Plane sends a `DataFlowStartMessage` (Data Plane Signaling) to the data
   plane, where the Kafka Extension requests a fresh OAuth2 access token from the OAuth Service via
   the Client Credentials flow (no refresh token) and creates a `DataAddress` containing the
   connection details (bootstrap servers, topic, security protocol, SASL mechanism, OAuth token,
   poll duration, and consumer group prefix). The Provider Control Plane then sends a
   `TransferStartMessage` with the complete DataAddress to the Consumer Control Plane, which
   constructs the final EDR.

![Sequence diagram EDC Kafka Extension start transfer process](diagrams/Sequence%20diagram%20EDC%20Kafka%20Extension%20start%20transfer%20process.png)

### C. Data Streaming

**Purpose:** establish a secure, token-based data stream between consumer and Kafka Service.

The Consumer Application requests the EDR, instantiates a Kafka consumer, and authenticates against
the Kafka Service (which validates the token). It then polls the topic for messages. The token is
short-lived; the consumer polls until it expires — there is no token refresh, a new transfer mints a
new token.

![Sequence diagram EDC Kafka Extension data streaming](diagrams/Sequence%20diagram%20EDC%20Kafka%20Extension%20data%20streaming.png)

### D. Suspending/Terminating

**Purpose:** securely suspend or terminate the transfer by revoking consumer credentials.

The Provider Control Plane signals the suspension or termination to the data plane. On suspend, the
Kafka Extension revokes the consumer's ACLs (when ACL management is enabled); the short-lived token
remains valid until it expires, and a resume re-creates the ACLs. On terminate, the extension
additionally calls the OAuth Service to revoke the token and removes it from the Vault. The Consumer
Control Plane is notified that the transfer has been suspended or terminated.

![Sequence diagram EDC Kafka Extension suspending-terminating](diagrams/Sequence%20diagram%20EDC%20Kafka%20Extension%20suspending-terminating.png)

## DataAddress Schema

A Kafka asset is published with a `DataAddress` of type `KafkaBroker`:

| Property | Description | Mandatory |
|---|---|---|
| `type` | Must be `KafkaBroker` | Yes |
| `https://w3id.org/edc/v0.0.1/ns/topic` | Kafka topic name | Yes |
| `https://w3id.org/edc/v0.0.1/ns/kafka.bootstrap.servers` | Kafka bootstrap servers | Yes |
| `https://w3id.org/edc/v0.0.1/ns/kafka.sasl.mechanism` | SASL mechanism (e.g., `OAUTHBEARER`) | Yes |
| `https://w3id.org/edc/v0.0.1/ns/kafka.security.protocol` | Security protocol (e.g., `SASL_PLAINTEXT`, `SASL_SSL`) | Yes |
| `https://w3id.org/edc/v0.0.1/ns/tokenUrl` | OAuth2 token endpoint URL | Yes |
| `https://w3id.org/edc/v0.0.1/ns/clientId` | OAuth2 client ID | Yes |
| `https://w3id.org/edc/v0.0.1/ns/clientSecretKey` | Vault entry key for the OAuth2 client secret | Yes |
| `https://w3id.org/edc/v0.0.1/ns/revokeUrl` | OAuth2 token revocation endpoint | No |
| `https://w3id.org/edc/v0.0.1/ns/kafka.poll.duration` | ISO-8601 consumer poll duration (default `PT1S`) | No |
| `https://w3id.org/edc/v0.0.1/ns/kafka.group.prefix` | Consumer group prefix the consumer is authorized to use; also scopes the consumer-group ACL. Defaults to the consumer participant id when omitted. | No |

> Property keys may be written as full IRIs (as in the table above) or with the `edc:` prefix (as in the
> example below); both are equivalent after JSON-LD expansion.

Example asset registration:

```json
{
  "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
  "@type": "Asset",
  "@id": "kafka-asset-1",
  "properties": { "name": "My Kafka Stream" },
  "dataAddress": {
    "type": "KafkaBroker",
    "edc:topic": "my-topic",
    "edc:kafka.bootstrap.servers": "kafka:9092",
    "edc:kafka.sasl.mechanism": "OAUTHBEARER",
    "edc:kafka.security.protocol": "SASL_PLAINTEXT",
    "edc:tokenUrl": "http://keycloak:8080/realms/kafka/protocol/openid-connect/token",
    "edc:revokeUrl": "http://keycloak:8080/realms/kafka/protocol/openid-connect/revoke",
    "edc:clientId": "edc-provider",
    "edc:clientSecretKey": "edc-provider-secret"
  }
}
```

## Configuration

### EDC data plane properties

| Property | Description | Default |
|---|---|---|
| `edc.dataplane.kafka.acl.enabled` | Enable Kafka ACL management | `false` |
| `edc.dataplane.kafka.acl.bootstrap.servers` | Kafka broker addresses for admin ACL operations | — |
| `edc.dataplane.kafka.acl.security.protocol` | Security protocol for the admin client | `PLAINTEXT` |
| `edc.dataplane.kafka.acl.sasl.mechanism` | SASL mechanism for the admin client | — |
| `edc.dataplane.kafka.acl.sasl.jaas.config` | JAAS config for the admin client | — |

> **Note:** The bootstrap servers, security protocol, and SASL settings are only required when ACL
> management is enabled (`edc.dataplane.kafka.acl.enabled=true`).

### Kafka broker

Configure the Kafka broker for OAuth/SASL authentication:

```properties
# KRaft mode (no ZooKeeper)
kafka.process.roles=broker,controller
kafka.controller.quorum.voters=1@kafka:29093
kafka.controller.listener.names=CONTROLLER

# Listeners & Protocols
kafka.listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,OIDC:SASL_PLAINTEXT
kafka.listeners=PLAINTEXT://kafka:29092,CONTROLLER://kafka:29093,OIDC://0.0.0.0:9092
kafka.advertised.listeners=PLAINTEXT://kafka:29092,OIDC://kafka:9092
kafka.inter.broker.listener.name=PLAINTEXT

# Enable SASL/OAUTHBEARER authentication
kafka.sasl.enabled.mechanisms=OAUTHBEARER
kafka.sasl.oauthbearer.jwks.endpoint.url=http://keycloak:8080/realms/kafka/protocol/openid-connect/certs
kafka.sasl.oauthbearer.token.endpoint.url=http://keycloak:8080/realms/kafka/protocol/openid-connect/token
kafka.sasl.oauthbearer.expected.audience=account
```

### Keycloak

Set up a realm (e.g. `kafka`) with clients for the EDC provider (`edc-provider`) and consumer
(`edc-consumer`), configure client credentials (client id + secret) for each, and set an appropriate
token TTL (recommended: 5 minutes).

## Security and Token Model

### Authentication flow

The extension uses the OAuth2 Client Credentials flow:

1. When a transfer process starts, the extension calls the OAuth2 token endpoint with the provider's
   client credentials.
2. The returned JWT is stored in the EDC Vault keyed by the transfer process id.
3. The token is included in the EDR sent to the consumer.
4. On terminate, the extension calls the token revocation URL (if configured) and deletes the token
   from the Vault. On suspend, broker access is cut by revoking the consumer's ACLs (when ACL
   management is enabled) while the short-lived token simply expires.

### Token expiry and revocation

The implementation does not use refresh tokens — each transfer mints a single short-lived access
token, so access naturally ends when the token expires. Configure the token TTL in the OAuth2
provider accordingly (recommended: 5 minutes).

Because the broker validates the access token by signature and expiry (not by a revocation lookup), a
token that has been revoked at the OAuth2 server can remain usable at the broker until it expires.
Immediate broker-level cutoff is therefore provided by **ACL revocation** when ACL management is
enabled; otherwise access ends at the token's TTL.

### Kafka ACL management (optional)

When `edc.dataplane.kafka.acl.enabled=true`:

1. On transfer start, the extension extracts the `sub` claim from the JWT (used as the Kafka
   principal, `User:<sub>`).
2. It creates three ACL bindings for that principal: `READ` and `DESCRIBE` on the topic, and `READ` on
   the consumer group prefix (the `kafka.group.prefix`, defaulting to the consumer participant id) — the same
   prefix handed to the consumer in the EDR, so the broker grant matches what the consumer is told to
   use.
3. On suspend/terminate, the ACLs are revoked immediately — closing the access window even before the
   token expires. On resume they are re-created.

See the [Hybrid OAuth2 + Kafka ACL Security](../decision-records/kafka-streaming/2025-07-11-kafka-hybrid-acl-security/README.md)
decision record for the rationale.

### Transport encryption (SASL_SSL)

Because the extension allows topic consumption across company borders, end-to-end encryption is
recommended for production: switch the security protocol from `SASL_PLAINTEXT` to `SASL_SSL` (set
`kafka.security.protocol` accordingly in the data address). With the default client configuration all
public CA-signed certificates are accepted; the consumer client can also be configured to trust a
custom certificate.

## Interoperability and Standards

The extension does not change anything related to IATP, DSP, or policy definitions, ensuring
conformity to [CX-0018 Dataspace Connectivity v.3.1.0](https://catenax-ev.github.io/docs/standards/CX-0018-DataspaceConnectivity)
(chapters 2.1, 2.3, 2.4 and 2.5).

Since the extension introduces the new transfer type `KafkaBroker-PULL`, the standard should be extended
by this type. An example of such an extension:

> 2.2.3 KafkaBroker-PULL
>
> A Consumer MUST send a `dspace:TransferRequestMessage` with `dct:format:dspace:KafkaBroker-PULL`.
>
> A Provider MUST send a `dspace:TransferStartMessage` with sufficient information in the
> `dspace:dataAddress` property so that a client connection to the `dspace:endpoint` may succeed when
> initialized with the properties `groupPrefix` and `topic`.
>
> A Provider Connector MUST ensure that the requested backend system has sufficient context from the
> negotiation to evaluate the legitimacy of the request.
>
> A Consumer may then use the provided data to execute requests against the endpoint. Despite the
> token, the endpoint still has the right to refuse serving a request — for instance when a consumer
> requests a different topic than the one specified in the `dspace:dataAddress`.

For background on EDC extension development, see the
[EDC Contributors Manual](https://eclipse-edc.github.io/documentation/for-contributors/), in
particular [Modules, Runtimes, and Components](https://eclipse-edc.github.io/documentation/for-adopters/modules-runtimes-components/),
[Extensions](https://eclipse-edc.github.io/documentation/for-adopters/extensions/), and the
[Data Plane Signaling interface](https://eclipse-edc.github.io/documentation/for-contributors/data-plane/data-plane-signaling/).

## Troubleshooting

**Connection issues**
- Verify Kafka bootstrap servers are reachable from the EDC data plane.
- Check that the security protocol and SASL mechanism match the broker configuration.

**Authentication issues**
- Verify the OAuth2 client credentials are correct and stored in the Vault.
- Check that the token URL is reachable from the EDC data plane.
- Confirm the Vault key name matches `clientSecretKey` in the data address.

**ACL issues** (only when ACL management is enabled)
- Verify the admin client has superuser privileges in Kafka.
- Confirm the JWT contains a `sub` claim (used as the Kafka principal).
- Check that the Kafka broker has an authorizer configured (`StandardAuthorizer`).

## Decision records

- [Hybrid OAuth2 + Kafka ACL security](../decision-records/kafka-streaming/2025-07-11-kafka-hybrid-acl-security/README.md)
- [Kafka on Kubernetes: Strimzi vs Bitnami](../decision-records/kafka-streaming/2025-07-17-kafka-on-kubernetes/README.md)
- [Kafka streaming extension migration](../decision-records/kafka-streaming/2026-04-30-kafka-streaming-extension/README.md)

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2025 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
