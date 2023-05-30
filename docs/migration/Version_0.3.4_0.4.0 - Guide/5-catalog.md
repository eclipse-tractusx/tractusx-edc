# Fetching provider's Catalog

### Old plain JSON Schema

```json
{
  "protocol" : "ids-protocol-http",
  "providerUrl": "http://provider-control-plane:8282/api/v1/ids",
  "querySpec": null
}
```

### New JSON-LD Document

> Please note: In our samples, properties **WILL NOT** be explicitly namespaced, and internal nodes **WILL NOT** be typed, relying on `@vocab` prefixing and root schema type inheritance respectively.


```json
{
  "@context": {
    "vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "protocol" : "dataspace-protocol-http",
  "providerUrl": "http://provider-control-plane:8282/api/v1/dsp",
  "querySpec": null
}
```

### Request
In this case we fetch a provider catalog without using `queryspec`.

```bash
curl -X POST "${MANAGEMENT_URL}/v2/contractdefinitions" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "vocab": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "protocol" : "dataspace-protocol-http",
              "providerUrl": "http://provider-control-plane:8282/api/v1/dsp",
              "querySpec": null
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'