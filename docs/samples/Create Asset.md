# Create Asset

This document will showcase how to create an asset with the new management API.

> Please note: Before running the examples the corresponding environment variables must be set.
> How such an environment can be setup locally is documented in [chapter 1](#1-optional---local-setup).

## Table of Content

1. [Optional - Local Setup](#1-optional---local-setup)
2. [Terminology](#2-terminology)
3. [Values](#3-values)
4. [Simple Asset](#4-simple-asset)
5. [Custom Property Asset](#5-custom-property-asset)
6. [Private Property Asset](#6-private-property-asset)
7. [Complex Property Asset](#7-complex-property-asset)

## 1. Optional - Local Setup

## 2. Terminology
| Connector           | New Protocol (DCAT Catalogs) | Description                                                   |
|---------------------|------------------------------|---------------------------------------------------------------|
| Asset Entry         | Asset                        | Contains the Asset ID and its Data Address.                   |
| Contract Definition | Asset Entry / Dataset        | Contains an Asset () that is offered and covered by a Policy. |

## 3. Values
| Key      | Description |
|----------|-------------|
| @context |             |
| @vocab   |             |
| edc      |             |

## 4. Simple Asset

````json
{
  "https://w3id.org/edc/v0.0.1/ns/asset":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/Asset",
    "@id":"some-asset-id"
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/DataAddress",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/type":"test-type",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/keyName":"test-key-name"
  }
}
````

```bash
curl -X POST "${CON_DATAMGMT_URL}/management/v2/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
                "https://w3id.org/edc/v0.0.1/ns/asset":{
                    "@context":{
                        "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
                        "edc":"https://w3id.org/edc/v0.0.1/ns/"
                    },
                    "@type":"https://w3id.org/edc/v0.0.1/ns/Asset",
                    "@id":"some-asset-id"
                },
                "https://w3id.org/edc/v0.0.1/ns/dataAddress":{
                    "@context":{
                        "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
                        "edc":"https://w3id.org/edc/v0.0.1/ns/"
                    },
                    "@type":"https://w3id.org/edc/v0.0.1/ns/DataAddress",
                    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/type":"test-type",
                    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/keyName":"test-key-name"
                }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```

## 5. Custom Property Asset
````json
{
  "https://w3id.org/edc/v0.0.1/ns/asset":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/Asset",
    "@id":"some-asset-id",
    "properties":{
      "name":"some-asset-name",
      "description":"some description",
      "edc:version":"0.2.1",
      "contenttype":"application/json"
    }
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/DataAddress",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/type":"test-type",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/keyName":"test-key-name"
  }
}
````

## 6. Private Property Asset

````json
{
  "https://w3id.org/edc/v0.0.1/ns/asset":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/Asset",
    "@id":"some-asset-id",
    "properties":{
      "name":"some-asset-name",
      "description":"some description",
      "edc:version":"0.2.1",
      "contenttype":"application/json"
    },
    "https://w3id.org/edc/v0.0.1/ns/privateProperties":{
      "test-prop":"test-val"
    }
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/DataAddress",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/type":"test-type",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/keyName":"test-key-name"
  }
}
````

## 7. Complex Property Asset

````json
{
  "https://w3id.org/edc/v0.0.1/ns/asset":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/Asset",
    "@id":"some-asset-id",
    "properties":{
      "name":"some-asset-name",
      "description":"some description",
      "edc:version":"0.2.1",
      "contenttype":"application/json",
      "payload":{
        "@type":"customPayload",
        "name":"max",
        "age":34
      }
    }
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress":{
    "@context":{
      "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
      "edc":"https://w3id.org/edc/v0.0.1/ns/"
    },
    "@type":"https://w3id.org/edc/v0.0.1/ns/DataAddress",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/type":"test-type",
    "https://w3id.org/edc/v0.0.1/ns/https://w3id.org/edc/v0.0.1/ns/keyName":"test-key-name"
  }
}
````