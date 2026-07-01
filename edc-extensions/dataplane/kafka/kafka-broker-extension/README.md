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
broker-level authorization for each transfer through a Kafka admin client. The following settings configure
that admin client (see also the [Kafka Streaming documentation](../../../../docs/development/kafka-streaming/README.md#configuration)):

| Property                                    | Description                                       | Default     |
|---------------------------------------------|---------------------------------------------------|-------------|
| `edc.dataplane.kafka.acl.enabled`           | Enable Kafka ACL management.                      | `false`     |
| `edc.dataplane.kafka.acl.bootstrap.servers` | Kafka broker addresses for admin ACL operations.  | —           |
| `edc.dataplane.kafka.acl.security.protocol` | Security protocol for the admin client.           | `PLAINTEXT` |
| `edc.dataplane.kafka.acl.sasl.mechanism`    | SASL mechanism for the admin client.              | —           |
| `edc.dataplane.kafka.acl.sasl.jaas.config`  | JAAS config for the admin client.                 | —           |

The bootstrap servers, security protocol, and SASL settings are only required when ACL management is
enabled (`edc.dataplane.kafka.acl.enabled=true`).

## DataAddress schema

When creating a Kafka asset, use the following properties in the `DataAddress`:

| Key                       | Description                                                                                       | Mandatory |
|---------------------------|---------------------------------------------------------------------------------------------------|-----------|
| `type`                    | Identifier of Kafka data address. Must be `KafkaBroker`.                                          | yes       |
| `topic`                   | The Kafka topic the consumer is allowed to poll.                                                  | yes       |
| `kafka.bootstrap.servers` | Kafka bootstrap servers.                                                                          | yes       |
| `kafka.sasl.mechanism`    | SASL mechanism, typically `OAUTHBEARER`.                                                          | yes       |
| `kafka.security.protocol` | `SASL_PLAINTEXT` or `SASL_SSL`.                                                                   | yes       |
| `kafka.poll.duration`     | ISO-8601 polling duration (e.g. `PT10S`). Defaults to 1 second.                                   | no        |
| `kafka.group.prefix`      | Consumer group prefix the consumer is authorized to use. Defaults to the consumer participant id. | no        |
| `tokenUrl`                | OAuth2 token endpoint for retrieving access tokens.                                               | yes       |
| `revokeUrl`               | OAuth2 token revocation endpoint. If omitted, token revocation is skipped.                        | no        |
| `clientId`                | OAuth2 client ID.                                                                                 | yes       |
| `clientSecretKey`         | Vault entry containing the OAuth2 client secret for `clientId`.                                   | yes       |

The `clientSecretKey` property must point to a vault entry holding the OAuth2
client secret. The client must have permission to read the configured topic.

## Example: register a Kafka asset

```json
{
  "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
  "@id": "kafka-asset-1",
  "properties": { "name": "Kafka stream" },
  "dataAddress": {
    "type": "KafkaBroker",
    "topic": "shopfloor-events",
    "kafka.bootstrap.servers": "kafka.example.com:9092",
    "kafka.sasl.mechanism": "OAUTHBEARER",
    "kafka.security.protocol": "SASL_PLAINTEXT",
    "kafka.poll.duration": "PT5S",
    "kafka.group.prefix": "consumer-",
    "tokenUrl": "https://oauth.example.com/token",
    "revokeUrl": "https://oauth.example.com/revoke",
    "clientId": "kafka-broker-client",
    "clientSecretKey": "kafka-broker-client-secret"
  }
}
```

## Further reading

- ADR: [`docs/development/decision-records/kafka-streaming/2026-04-30-kafka-streaming-extension`](../../../../docs/development/decision-records/kafka-streaming/2026-04-30-kafka-streaming-extension/README.md)
- Documentation: [`docs/development/kafka-streaming`](../../../../docs/development/kafka-streaming/README.md)
