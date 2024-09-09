# Backport of the Azure Blob Storage Provisioner

This module is a backport, that means the contents of
the [upstream module](https://github.com/eclipse-edc/Technology-Azure/tree/main/extensions/control-plane/provision/provision-blob)
are copied here.

## Defining an `Asset` located in AzBlob

Create an asset similar to this on the provider connector's Management API:

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "blob-test-asset",
  "properties": {},
  "dataAddress": {
    "keyName": "provider-key-alias",
    "type": "AzureStorage",
    "@type": "DataAddress",
    "name": "transfer-test",
    "container": "provider-container",
    "account": "provider",
    "blobPrefix": "folder/"
  }
}
```

Explanation:

- `keyName`: alias under which the Storage Account Key is located in the provider's vault, stored as plain `String`
- `type`: **must** be `AzureStorage`
- `container`: the name of the source AzBlob container on the provider side
- `account`: the name of the AzBlob account on the provider side
- `blobPrefix`: in case all contents of a "folder" are to be copied, this is the "folder name"

## Creating an AzBlob-to-AzBlob transfer request

Assuming the contract negotiation has succeeded, execute a transfer process request on the consumer's Management API
endpoint with the following content:

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "TransferRequest",
  "protocol": "dataspace-protocol-http",
  "contractId": "416aed9c-7258-45c8-bdee-c09d5da7c255",
  "connectorId": "PROVIDER-BPN",
  "counterPartyAddress": "http://localhost:40950/protocol",
  "dataDestination": {
    "@type": "https://w3id.org/edc/v0.0.1/ns/DataAddress",
    "https://w3id.org/edc/v0.0.1/ns/type": "AzureStorage",
    "https://w3id.org/edc/v0.0.1/ns/properties": {
      "https://w3id.org/edc/v0.0.1/ns/type": "AzureStorage",
      "https://w3id.org/edc/v0.0.1/ns/account": "consumer",
      "https://w3id.org/edc/v0.0.1/ns/container": "consumer-container"
    }
  },
  "transferType": "AzureStorage-PUSH"
}
```

Explanation:
- `type`: **must** be `AzureStorage`
- `account`: the consumer account name
- `container`: the destination container in the consumer's Azure Blob account
- `transferType`: **must** be `AzureStorage-PUSH` for the provider to push the data into the consumer's AzBlob container

> Note that the Storage Account Key on the consumer side is expected in the Vault under the alias `<ACCOUNTNAME>-key1`,
here that would be `consumer-key1`. The key **must** be the raw Account Key (no SAS token), stored as plain String.

The AzBlob provisioner on the consumer side will generate a temporary SAS token for the consumer container (
`"consumer-container"`) and send it to the provider in a DSP `TransferRequestMessage`. The provider will then store it
in its own Vault, where it gets resolved from the provider Data Plane.