# Creating an Asset

## Old plain JSON Schema

```json
{
  "asset": {
    "id": "asset-id",
    "properties": {
      "name": "asset-name",
      "description": "asset-description",
      "version": "0.0.1",
      "contenttype": "application/json"
    }
  },
  "dataAddress": {
    "type": "asset-address-type",
    "keyName": "asset-key-name"
  }
}
```

## New JSON-LD Document

> Please note: In our samples, properties **WILL NOT** be explicitly namespaced, and internal nodes **WILL NOT** be typed, relying on `@vocab` prefixing and root schema type inheritance respectively.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type": "AssetEntryDto",
  "asset": {
    "@id": "asset-id",
    "properties": {
      "name": "asset-name",
      "description": "asset-description",
      "version": "0.0.1",
      "contenttype": "application/json"
    },
    "privateProperties": {
      "pvt-prop-1": "prt-prov-val-1",
      "pvt-prop-2": "prt-prov-val-2"
    }
  },
  "dataAddress": {
    "type": "asset-address-type",
    "keyName": "asset-key-name"
  }
}
```

A new addition are the `privateProperties`.
Private properties will not be sent through the dataplane and are only accessible via the management API.
This enables the storage of additional information pertaining the asset, that is not relevant for the consumer, but is nonetheless useful for the provider.
Private properties are stores inside the `privateProperties` field.
> Please note: `privateProperties` are entirely optional and the field is not required for creating or updating an asset.

> Please note: `privateProperties` are entirely optional and the field is not required for creating or updating an asset.

## Request

In this case we generate a very simple asset, that only contains the minimum in terms of information.
For this we need both an asset and a data address, which together form an asset entry.

```bash
curl -X POST "${MANAGEMENT_URL}/v2/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "@type": "AssetEntryDto",
              "asset": {
                "@id": "asset-id"
              },
              "dataAddress": {
                "type": "asset-address-type",
                "keyName": "asset-key-name"
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```
