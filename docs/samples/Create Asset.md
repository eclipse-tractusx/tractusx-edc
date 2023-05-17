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

TODO link to a 0.4.x example setup.

## 2. Terminology

With the migration to a new protocol and with it new specifications, DCAT Catalogs in the context of assets, there is a need to clarify the terminology that is used.
In the table below, relevant terms from the connector and their new counterparts are listed and explained with a short description.

| Connector           | New Protocol (DCAT Catalogs) | Description                                                   |
|---------------------|------------------------------|---------------------------------------------------------------|
| Asset Entry         | Asset                        | Contains the Asset ID and its Data Address.                   |
| Contract Definition | Asset Entry / Dataset        | Contains an Asset () that is offered and covered by a Policy. |

Generally, this documentation uses the connector terminology unless otherwise specified.

## 3. Values

Since some keys which are required in requests for the new management API aren't self-explanatory when you first see them, a short explanation is given below.

| Key      | Description |
|----------|-------------|
| @context |             |
| @vocab   |             |
| edc      |             |

## 4. Simple Asset

The first use-case is to generate a very simple asset, that only contains the minimum in terms of information.
For this we need both an asset and a data address, which together form an asset entry.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "https://w3id.org/edc/v0.0.1/ns/asset": {
    "@type": "Asset",
    "@id": "some-asset-id"
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
    "@type": "DataAddress",
    "type": "test-type",
    "keyName": "test-key-name"
  }
}
```

The asset entry is then sent to the management API of the connector through a POST request.

**CON_DATAMGMT_URL** specifies the URL of the management API.
The path `/management/v2` allows access to the functionality of the new management API.
Adding `/assets` then leads to the functionality pertaining assets.

> Please note: `/v2` is only a temporary part of the path and will be discarded once the migration to the new protocol is finished and the old API is taken out of service.
> Once that is done, the new management API can be reached through `/management`.

```bash
curl -X POST "${CON_DATAMGMT_URL}/management/v2/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "edc": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "https://w3id.org/edc/v0.0.1/ns/asset": {
                "@type": "Asset",
                "@id": "some-asset-id"
              },
              "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
                "@type": "DataAddress",
                "type": "test-type",
                "keyName": "test-key-name"
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```

In the following chapters, only the contents of the request body will change in accordance to the asset payload.

## 5. Custom Property Asset

In the real world, an asset usually doesn't consist of the bare minimum but also contains custom properties, which store additional information.
The basic use-case is that each property only consists of primitive datatypes and therefore simple key/value pairs.
Those pairs are then stored inside the `properties` field.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "https://w3id.org/edc/v0.0.1/ns/asset": {
    "@type": "Asset",
    "@id": "some-asset-id",
    "properties": {
      "name": "some-asset-name",
      "description": "some description",
      "edc:version": "0.2.1",
      "contenttype": "application/json"
    }
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
    "@type": "DataAddress",
    "type": "test-type",
    "keyName": "test-key-name"
  }
}
```

```bash
curl -X POST "${CON_DATAMGMT_URL}/management/v2/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "edc": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "https://w3id.org/edc/v0.0.1/ns/asset": {
                "@type": "Asset",
                "@id": "some-asset-id",
                "properties": {
                  "name": "some-asset-name",
                  "description": "some description",
                  "edc:version": "0.2.1",
                  "contenttype": "application/json"
                }
              },
              "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
                "@type": "DataAddress",
                "type": "test-type",
                "keyName": "test-key-name"
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```

## 6. Private Property Asset

A new addition are the private properties.
Private properties will not be sent through the dataplane and are only accessible via the management API.
This enables the storage of additional information pertaining the asset, that is not relevant for the consumer, but is nonetheless useful for the provider.
Private properties are stores inside the `privateProperties` field.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "https://w3id.org/edc/v0.0.1/ns/asset": {
    "@type": "Asset",
    "@id": "some-asset-id",
    "properties": {
      "name": "some-asset-name",
      "description": "some description",
      "edc:version": "0.2.1",
      "contenttype": "application/json"
    },
    "https://w3id.org/edc/v0.0.1/ns/privateProperties": {
      "test-prop": "test-val"
    }
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
    "@type": "DataAddress",
    "type": "test-type",
    "keyName": "test-key-name"
  }
}
```

```bash
curl -X POST "${CON_DATAMGMT_URL}/management/v2/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "edc": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "https://w3id.org/edc/v0.0.1/ns/asset": {
                "@type": "Asset",
                "@id": "some-asset-id",
                "properties": {
                  "name": "some-asset-name",
                  "description": "some description",
                  "edc:version": "0.2.1",
                  "contenttype": "application/json"
                },
                "https://w3id.org/edc/v0.0.1/ns/privateProperties": {
                  "test-prop": "test-val"
                }
              },
              "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
                "@type": "DataAddress",
                "type": "test-type",
                "keyName": "test-key-name"
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```
## 7. Complex Property Asset

Besides primitive datatypes, complex JSON objects can also be stored inside the properties.
An example is the `payload` field and its contents.
Here the `payload` contains the name and age of a person.

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "https://w3id.org/edc/v0.0.1/ns/asset": {
    "@type": "Asset",
    "@id": "some-asset-id",
    "properties": {
      "name": "some-asset-name",
      "description": "some description",
      "edc:version": "0.2.1",
      "contenttype": "application/json",
      "payload": {
        "@type": "customPayload",
        "name": "max",
        "age": 34
      }
    }
  },
  "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
    "@type": "DataAddress",
    "type": "test-type",
    "keyName": "test-key-name"
  }
}
```

```bash
curl -X POST "${CON_DATAMGMT_URL}/management/v2/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
              "@context": {
                "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                "edc": "https://w3id.org/edc/v0.0.1/ns/"
              },
              "https://w3id.org/edc/v0.0.1/ns/asset": {
                "@type": "Asset",
                "@id": "some-asset-id",
                "properties": {
                  "name": "some-asset-name",
                  "description": "some description",
                  "edc:version": "0.2.1",
                  "contenttype": "application/json",
                  "payload": {
                    "@type": "customPayload",
                    "name": "max",
                    "age": 34
                  }
                }
              },
              "https://w3id.org/edc/v0.0.1/ns/dataAddress": {
                "@type": "DataAddress",
                "type": "test-type",
                "keyName": "test-key-name"
              }
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```