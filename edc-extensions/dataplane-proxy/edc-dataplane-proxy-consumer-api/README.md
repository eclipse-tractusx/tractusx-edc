# DataPlane Proxy Consumer API

This is an API extension that interacts with the EDR/cache for directly fetching the data
without knowing the EDR.

It contains only one endpoint with `POST` for fetching data:

The path is `<proxyContext>/aas/request` and the body is something like this example:

```json
{
  "assetId": "1",
  "endpointUrl": "http://localhost:8181/api/gateway/aas/test"
}
```

The body should contain the `assetId` or the `transferProcessId` which identify the data that we want to fetch
and an `endpointUrl` which is the provider gateway on which the data is available. More info [here](../edc-dataplane-proxy-provider-api/README.md) on the gateway.

## Configuration

| Key                             | Required | Default       | Description                |
|---------------------------------|----------|--------------------------------------------|
| web.http.proxy.port             |          | 8186          |                            |
| web.http.proxy.path             |          | /proxy        |                            |
