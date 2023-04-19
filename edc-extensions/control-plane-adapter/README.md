# Control Plane Adapter Extension

The goal of this extension is to simplify the process of retrieving data out of EDC. It returns an `EndpointDataReference` object, hiding all the communication details for contract offers, contract negotiation process and retrieving `EndpointDataReference` from EDC controlplane.

Additional requirements, that affects the architecture of the extension:

- can return data both in SYNC and ASYNC mode (currently only SYNC endpoint available)
- can be persistent, so that process can be restored from the point where it was before application was stopped  
- scaling horizontally (when persistence is added to configuration)
- can retry failed part of the process (no need to start the process from the beginning)

## Configuration

| Key                                                | Description                                                                                                                                                                                              | Mandatory | Default |
|:---------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|---------|
| `edc.cp.adapter.default.message.retry.number`      | Number of retries of a message, in case of an error, within the internal process of retrieving DataReference                                                                                             | no        | 3       |
| `edc.cp.adapter.default.sync.request.timeout`      | Timeout for synchronous request (in seconds), after witch 'timeout' error will be returned to the requesting client                                                                                      | no        | 20      |
| `edc.cp.adapter.messagebus.inmemory.thread.number` | Number of threads running within the in-memory implementation of MessageBus                                                                                                                              | no        | 10      |
| `edc.cp.adapter.reuse.contract.agreement`          | Turn on/off reusing of existing contract agreements for the specific asset. Once the contract is agreed, the second request for the same asset will reuse the agreement (if exists) pulled from the EDC. | no        | true    |
| `edc.cp.adapter.cache.catalog.expire.after`        | Number of seconds, after witch previously requested catalog will not be reused, and will be removed from catalog cache                                                                                   | no        | 300     |
| `edc.cp.adapter.catalog.request.limit`             | Maximum number of items taken from Catalog within single request. Requests are repeated until all offers of the query are retrieved                                                                      | no        | 100     |

By default, the extension works in "IN MEMORY" mode. This setup has some limitations:

- It can work only within single EDC instance. If CP-adapter requests are handled by more than one EDC, data flow may be broken.
- If the EDC instance is restarted, all running processes are lost.

To run CP-Adapter in "PERSISTENT" mode, You need to create a proper tables with [this](docs/schema.sql) script, and add the following configuration values to your controlplane EDC properties file:

| Key                                 | Description          |
|-------------------------------------|----------------------|
| `edc.datasource.cpadapter.name`     | data source name     |
| `edc.datasource.cpadapter.url`      | data source url      |
| `edc.datasource.cpadapter.user`     | data source user     |
| `edc.datasource.cpadapter.password` | data source password |

## How to use it

1. Client sends a GET request with two parameters: assetId and the url of the provider controlplane:

   ```plain
   {controlplaneUrl}:{web.http.management.port}/{web.http.management.path}/adapter/asset/sync/{assetId}?providerUrl={providerUrl}
   ```

   | Name                       | Description                                                                      |
   |----------------------------|----------------------------------------------------------------------------------|
   | `controlplaneUrl`          | The URL where the control plane of the consumer connector is available           |
   | `web.http.management.port` | Port of the management API provided by the control plane                         |
   | `web.http.management.path` | Path of the management API provided by the control plane                         |
   | `assetId`                  | ID of the wanted asset                                                           |
   | `providerUrl`              | URL pointing to the `data` endpoint of the IDS context of the provider connector |

   The example ULR could be:

   ```plain
   http://localhost:9193/api/v1/data/adapter/asset/sync/123?providerUrl=http://localhost:8182/api/v1/ids/data
   ```

   Optional request parameters, that overwrite the settings for a single request:

   | Name                     | Description                                                                                                                                                                                                            |
   |--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
   | `contractAgreementId`    | Defines the ID of existing contract agreement, that should be reused for retrieving the asset. If parameter is specified, but contract is not found, 404 error will be returned.                                       |
   | `contractAgreementReuse` | Similar to `edc.cp.adapter.reuse.contract.agreement` option allows to turn off reusing of existing contracts, but on a request level. Set the parameter value to 'false' and new contract agrement will be negotiated. |
   | `timeout`                | Similar to `edc.cp.adapter.default.sync.request.timeout`, defines the maximum time of the request. If data is not ready, time out error will be returned.                                                              |

   The controller is registered under the context alias of the Management API. The authentication depends on the configuration of the Management API.
   To find out more please visit:

   - [Management API Documentation](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/api/management-api)
   - [Management API Configuration Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/api/management-api-configuration)

2. `EndpointDataReference` object is returned. Example of the `EndpointDataReference` response:

    ```json
    {
      "id": "ee8b758a-4b02-4cca-bb37-d0256b4638e7",
      "endpoint": "http://consumer-dataplane:9192/publicsubmodel?provider-connector-url=...",
      "authKey": "Authorization",
      "authCode": "eyJhbGciOiJSUzI1NiJ9.eyJkYWQiOi...",
      "properties": {
        "cid": "1:b2367617-5f51-48c5-9f25-e30a7299235c"
      }
    }
    ```

3. Client, using the `EndpointDataReference`, retrieves the Asset through dataplane.

   Example of the dataplane GET request, to retrieve Asset, with `EndpointDataReference` information:

   ```plain
   url:          http://consumer-dataplane:9192/publicsubmodel?provider-connector-url=...                {endpoint}
   header:       Authorization:eyJhbGciOiJSUzI1NiJ9.eyJkYWQiOi...                                        {authKey:authCode}
   ```

### Internal design of the extension

![diagram](src/main/resources/control-plane-adapter.jpg)
