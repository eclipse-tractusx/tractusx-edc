# Dataplane to expose HTTP status code

## Decision

The dataplane API will expose the status codes and respective messages based on a new allowed status code range parameter. 

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. This is due to not exposing potentitally sensitive information. However, there is a need to allow the option of returning different http status codes that may reflect the real behaviour, easing troubleshooting.

To achieve it, a new `allowedStatusCodes` field will be added to the http data address in which it contains the accepted http status codes (and respective response messages) that the dataplane can return. The current behaviour will be kept as the default one.

## Approach

1. Include the logic needed from `data-plane-http` and `data-plane-http-spi` (like `HttpDataSource`, `HttpRequestParams` or `HttpDataAddress`) upstream modules in the Tractus-X EDC distribution. 
2. In the `HttpRequestParams` include the new optional parameter `allowedStatusCodes`. If empty, the default behaviour will be kept (2XX or 5XX).
3. In the `HttpDataSource` included implementation, update the `openPartStream()` overridden method to handle the error message to allow the return of established status code and respective message, based on chosen range.