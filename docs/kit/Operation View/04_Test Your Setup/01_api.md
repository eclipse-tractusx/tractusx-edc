# EDC API Examples

## API Spec

The API spec of the EDC is constantly evolving.
The full API documentation for each release can be viewed on [management-api](../Development View/02_OpenAPI/management-api/management-api.info.mdx).
The following are some example API calls for common use cases.
They assume the default parameters from the previous local setup.

## Create an Asset

All objects in EDC are created by posting their JSON-serialized representation to the respective API input.
Since most EDC objects are rather openly defined, most of the properties provided depend on the need of the individual user.
Assets are no exception here.

URL

```http request
POST http://localhost:8080/api/v1/assets/
```

Body

```json
{
  "asset": {
    "id": "asset1",
    "properties": {
      "exampleProperty": "exampleValue"
    }
  },
  "dataAddress": {
    "properties": {
      "baseUrl": "https://path.to/the_asset",
      "type": "HttpData"
    }
  }
}
```

## Request an Asset Catalog

To inspect the assets available to an EDC connector, we request its catalog.

URL

```http request
POST http://localhost:8080/api/v1/catalog/request
```

Body

```json
{
  "providerUrl": "www.example.provider",
  "querySpec": {
    "filter": "AvailableWithPolicyXYZ",
    "limit": 0,
    "offset": 0,
    "sortField": "id",
    "sortOrder": "ASC"
  }
}
```

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
