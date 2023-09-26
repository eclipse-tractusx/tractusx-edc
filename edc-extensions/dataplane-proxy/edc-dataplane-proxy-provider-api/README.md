# DataPlane Proxy Provider API

This extension provide additional dataplane extension for proxying requests to backends.
The configuration of the proxy can be found [here](../edc-dataplane-proxy-provider-core/README.md)

The provider proxy is mounted into the EDC default context, and it's available in the path `<defaultContext>/gateway`

The proxy will look for subPath in the request and match the subpath with the configured ones and forward
the rest of the path and query parameters.

For example:

with this URL `http://localhost:8181/api/gateway/aas/test` it will look for the `aas` alias in the configuration,
and it will compose the final url to call based on that configuration appending to it the remaining part of the path and query
parameters.

When the proxy receive a request, it must contain the EDR, which will be decoded with the `token` validation endpoint.

## Configuration

| Key                                        | Required | Default       | Description                                                 |
|--------------------------------------------|----------------------------------------------------------------------------------------|
| tx.dpf.provider.proxy.thread.pool          |          | 10            | Thread pool size for the provider data plane proxy gateway  |
| web.http.gateway.context                   |          | default       | Context to register the ProviderGatewayController into      |
