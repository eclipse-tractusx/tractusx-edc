# Kafka Broker Extension

## Overview

The Kafka Broker Extension is a data-plane extension that enables secure, dynamic
access to Kafka topics within the Tractus-X EDC. It allows data providers to share
Kafka streams with consumers while maintaining full control over access permissions
through per-transfer OAuth2 credentials.

## Sibling modules

This extension lives alongside two supporting modules under `edc-extensions/dataplane/kafka/`:

- **`data-address-kafka`** — defines the `KafkaBrokerDataAddressSchema` constants for
  Kafka data address properties.
- **`validator-data-address-kafka`** — validates that a `DataAddress` of type
  `KafkaBroker` carries all required properties before a transfer is initiated.

## Transfer type

The extension adds the **`KafkaBroker-PULL`** transfer type to the EDC data plane
via a provisioner and an EDR service. The provisioning lifecycle:

1. **start** — provisions a fresh OAuth2 access token via the Client Credentials
   flow, stores it in the provider vault keyed by the transfer process id, and
   returns an EDR containing the bootstrap servers, topic, security protocol, SASL
   mechanism, consumer group prefix, poll duration, and token. When ACL
   management is enabled, it also creates the consumer's Kafka ACLs.
2. **suspend** — revokes the consumer's ACLs (when ACL management is enabled), cutting
   broker access immediately; the short-lived token itself stays in the vault and remains
   valid until it expires. On **resume** the ACLs are re-created.
3. **terminate** — revokes the token at the OAuth2 server's revocation endpoint (if a
   `revokeUrl` is configured), removes it from the vault, and — when ACL management is
   enabled — revokes the consumer's ACLs.

## Configuration

Kafka ACL management is optional and disabled by default. When enabled, the data plane manages
broker-level authorization for each transfer through a Kafka admin client, configured via the
`edc.dataplane.kafka.acl.*` settings — see
[Configuration](../README.md#configuration).

## Further reading

The `KafkaBroker` data address schema, the full configuration reference (EDC data plane, Kafka
broker, Keycloak), the security and token model, the end-to-end transfer workflow, and how to
include the extension in a runtime are documented in
[Kafka Streaming Extension](../README.md).
