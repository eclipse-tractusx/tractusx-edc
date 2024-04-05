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

Alternatively if the `endpointUrl` is not known or the gateway on the provider side is not configured, it can be omitted and the `Edr#endpointUrl`
will be used. In this scenario if needed users can provide additional properties to the request for composing the final
url:

- `pathSegments` sub path to append to the base url
- `queryParams` query parameters to add to the url

Example with base url `http://localhost:8080/test`

```json
{
  "assetId": "1",
  "pathSegments": "/sub",
  "queryParams": "foo=bar"
}
```

The final url will look like `http://localhost:8080/test/sub?foo=bar` composed by the DataPlane manager with the Http request flow,

> Note: the endpoint is protected with configured `AuthenticationService`.

## Configuration

| Key                             | Required | Default       | Description                |
|---------------------------------|----------|--------------------------------------------|
| web.http.proxy.port             |          | 8186          |                            |
| web.http.proxy.path             |          | /proxy        |                            |
