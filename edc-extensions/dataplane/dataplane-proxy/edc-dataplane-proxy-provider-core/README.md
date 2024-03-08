# DataPlane Proxy Provider Core

This extension provide the base service and configuration for the DataPlane Provider Proxy.

## Configuration

| Key                                                | Required | Default       | Description                                           |
|----------------------------------------------------|----------------------------------------------------------------------------------|
| tx.dpf.proxy.gateway.alias.proxied.path          |X         | 10            | The backend URL to proxy                              |
| tx.dpf.proxy.gateway.alias.proxied.edr.forward   |          | false         | If the original EDR must be forwarded to the backend  |
| tx.dpf.proxy.gateway.alias.proxied.edr.headerKey |          | Edc-Edr       | The header name to use when forwarding the EDR        |

Where `alias` is the first part of the subpath after `gateway` mentioned [here](../edc-dataplane-proxy-provider-api/README.md)
