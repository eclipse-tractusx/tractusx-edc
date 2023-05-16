# Create Asset

This document will showcase how to create an asset with the new management API.

> Please note: Before running the examples the corresponding environment variables must be set.
> How such an environment can be setup locally is documented in [chapter 1](#1-optional---local-setup).

## Table of Content

1. [Optional - Local Setup](#1-optional---local-setup)
2. [Terminology](#2-terminology)
3. [Simple Asset](#3-simple-asset)
4. [Custom Property Asset](#4-custom-property-asset)
5. [Private Property Asset](#5-private-property-asset)
6. [Complex Property Asset](#6-complex-property-asset)

## 1. Optional - Local Setup

## 2. Terminology

## 3. Simple Asset

````json
{
  "@context":{
    "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
    "edc":"https://w3id.org/edc/v0.0.1/ns/"
  },
  "@type":"https://w3id.org/edc/v0.0.1/ns/Asset",
  "@id":"some-asset-id"
}
````

```bash
curl -X POST "${CON_DATAMGMT_URL}/management/v2/assets" \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
               "@context":{
                  "@vocab":"https://w3id.org/edc/v0.0.1/ns/",
                  "edc":"https://w3id.org/edc/v0.0.1/ns/"
               },
               "@type":"https://w3id.org/edc/v0.0.1/ns/Asset",
               "@id":"some-asset-id"
            }' \
    -s -o /dev/null -w 'Response Code: %{http_code}\n'
```

## 4. Custom Property Asset
````json
{
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
}
````

## 5. Private Property Asset

````json
{
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
}
````

## 6. Complex Property Asset

````json
{
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
}
````