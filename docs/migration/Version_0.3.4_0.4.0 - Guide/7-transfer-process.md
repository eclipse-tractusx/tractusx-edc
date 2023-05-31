# Initiation a Transfer Process

## Old plain JSON Schema

```json
{
  "assetId": "asset-id",
  "connectorAddress": "provider-dsp-url",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "properties": {
      "type": "HttpProxy"
    }
  },
  "managedResources": false,
  "privateProperties": {
    "receiverHttpEndpoint": "backend-service-endpoint"
  },
  "protocol": "ids-protocol-http",
  "transferType": {
    "contentType": "application/octet-stream",
    "isFinite": true
  }
}
```

### New JSON-LD Document

> Please note: In our samples, properties **WILL NOT** be explicitly namespaced, and internal nodes **WILL NOT** be typed, relying on `@vocab` prefixing and root schema type inheritance respectively.


```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "assetId": "asset-id",
  "connectorAddress": "provider-dsp-url",
  "contractId": "contract-agreement-id",
  "dataDestination": {
    "properties": {
      "type": "HttpProxy"
    }
  },
  "managedResources": false,
  "privateProperties": {
    "receiverHttpEndpoint": "backend-service-endpoint"
  },
  "protocol": "dataspace-protocol-http",
  "transferType": {
    "contentType": "application/octet-stream",
    "isFinite": true
  }
}
```

### Request
In this case we initiate a transfer process with the provider.

```bash
curl -X POST "${MANAGEMENT_URL}/v2/contractdefinitions" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "odrl": "http://www.w3.org/ns/odrl/2/"
              },
              "assetId": "asset-id",
              "connectorAddress": "provider-dsp-url",
              "contractId": "contract-agreement-id",
              "dataDestination": {
                "properties": {
                  "type": "HttpProxy"
                }
              },
              "managedResources": false,
              "privateProperties": {
                "receiverHttpEndpoint": "backend-service-endpoint"
              },
              "protocol": "dataspace-protocol-http",
              "transferType": {
                "contentType": "application/octet-stream",
                "isFinite": true
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'