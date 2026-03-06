# Connector Discovery Extension

## Overview
The Connector Discovery extension enables participants to discover other connectors in the dataspace.
The **connector-discovery-api** module provides a REST API and default implementation for **DID-based** discovery with **Dataspace Protocol (DSP) 2025-1**. 
It resolves connector endpoints from a participant’s DID document.

---

## connector-discovery-api

### Functionality
The module exposes a **v4alpha** Management API under base path `/v4alpha/connectordiscovery` and implements two discovery flows:

1. **DSP version params discovery**: Determine which DSP version to use and return the connection parameters (protocol, counterPartyId, counterPartyAddress).
2. **Connector services discovery**: For a participant (by DID), discover all published service and connectors endpoints from their DID document, then resolve version params for each.

#### Components
- **ConnectorDiscoveryExtension**: Registers the REST controller under the Management API context, registers JSON-LD transformers and validators for the request types.
- **ConnectorDiscoveryV4AlphaController**: Provides the endpoints described above.
- **DefaultConnectorDiscoveryServiceImpl**: DID-only, DSP 2025-1 implementation. Extends `BaseConnectorDiscoveryServiceImpl` with `supportedVersions = [Dsp2025Constants.V_2025_1_VERSION]` and enforces that `counterPartyId` must start with `did:` for both operations.
- **BaseConnectorDiscoveryServiceImpl** — Core logic:
  - **Version params**: Builds URL to counterparty’s `/.well-known/dspace-version`, uses an in-memory cache (key = version endpoint URL). On cache miss, GETs the well-known endpoint, parses `ProtocolVersions` JSON and returns a JSON object with `protocol`, `counterPartyId`, `counterPartyAddress`.
  - **Connector discovery**: Resolves the participant’s DID, reads the DID document’s `service` array, collects endpoints from entries with type `DataService`, merges with optional `knownConnectors`, then for each endpoint calls `discoverVersionParams` in parallel.

#### API Endpoints

| Method | Path | Description                                                                                              |
|--------|------|----------------------------------------------------------------------------------------------------------|
| `POST` | `/v4alpha/connectordiscovery/dspversionparams` | Discover DSP version params for a single connector endpoint (via `/.well-known/dspace-version`).         |
| `POST` | `/v4alpha/connectordiscovery/connectors` | Discover all service and connector endpoints from a participant’s DID document and their version params. |

Requests and responses are JSON-LD.

### Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `tx.edc.connector.discovery.cache.expiry` | `7200000` (2 hours) | Cache expiry for protocol version information in milliseconds. Used for the in-memory cache keyed by the counterparty’s `/.well-known/dspace-version` URL. |

---

### How to use in EDC
Add the connector-discovery-api module to the control-plane:
```kotlin
implementation(project(":edc-extensions:connector-discovery:connector-discovery-api"))
```

## Other modules
- **cx-connector-discovery**: Catena-X–specific implementation (BPNL identifiers, DSP 0.8 and 2025-1). Not covered in detail here; add as a dependency if you need BPNL-based discovery and multi-version DSP support.
