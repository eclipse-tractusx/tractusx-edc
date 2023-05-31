# Initiation a Contract Negotiation

### Old plain JSON Schema

```json
{
  "connectorAddress": "provider-dsp-url",
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-id",
  "providerId": provider-id",
  "offer": {
    "offerId": "offer-id",
    "assetId": "asset-id",
    "policy": {
      "permissions": [
        {
          "action": {
            "type": "USE"
          },
          "constraints": [
            {
              "leftExpression": {
                "value": "BusinessPartnerNumber"
              },
              "rightExpression": {
                "value": "BPN"
              },
              "operator": "EQ"
            }
          ]
        }
      ],
      "prohibition": [],
      "obligation": []
    }
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
  "@type": "NegotiationInitiateRequestDto",
  "connectorAddress": "provider-dsp-url",
  "protocol": "dataspace-protocol-http",
  "connectorId": "provider-id",
  "providerId": provider-id",
  "offer": {
    "offerId": "offer-id",
    "assetId": "asset-id",
    "policy": {
      "@type": "odrl:Set",
      "odrl:permission": {
        "odrl:target": "asset-id",
        "odrl:action": {
          "odrl:type": "USE"
        },
        "odrl:constraint": {
          "odrl:or": {
            "odrl:leftOperand": "BusinessPartnerNumber",
            "odrl:operator": "EQ",
            "odrl:rightOperand": "BPN"
          }
        }
      },
      "odrl:prohibition": [],
      "odrl:obligation": [],
      "odrl:target": "asset-id"
    }
  }
}
```

## Request

In this case we initiate a contract negotiations with the provider.

```bash
curl -X POST "${MANAGEMENT_URL}/v2/contractnegotiations" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
  "@context": {
                  "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                  "odrl": "http://www.w3.org/ns/odrl/2/"
                },
                "@type": "NegotiationInitiateRequestDto",
                "connectorAddress": "provider-dsp-url",
                "protocol": "dataspace-protocol-http",
                "connectorId": "provider-id",
                "providerId": provider-id",
                "offer": {
                  "offerId": "offer-id",
                  "assetId": "asset-id",
                  "policy": {
                    "@type": "odrl:Set",
                    "odrl:permission": {
                      "odrl:target": "asset-id",
                      "odrl:action": {
                        "odrl:type": "USE"
                      },
                      "odrl:constraint": {
                        "odrl:or": {
                          "odrl:leftOperand": "BusinessPartnerNumber",
                          "odrl:operator": "EQ",
                          "odrl:rightOperand": "BPN"
                        }
                      }
                    },
                    "odrl:prohibition": [],
                    "odrl:obligation": [],
                    "odrl:target": "asset-id"
                  }
                }
              }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'