# Dataplane to expose selected HTTP status code

## Decision

The dataplane API will expose the status codes and respective messages based on a new allowed status code range parameter. 

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. However, there is a requirement to allow the option of returning different http status codes (like 4XX) back to the consumer.

To achieve it, a new `proxyStatusCodes` field will be added to the http data address in which it contains the accepted http status codes (and respective response messages) that the dataplane can return. The current behaviour will be kept as the default one.

## Approach

1. Include the logic needed from `data-plane-http` and `data-plane-http-spi` (like `HttpDataSource`, `HttpRequestParams` or `HttpDataAddress`) upstream modules in the Tractus-X EDC distribution. 
2. In the `HttpRequestParams` include the new optional parameter `proxyStatusCodes`. If empty, the default behaviour will be kept (2XX or 5XX).
3. In the `HttpDataSource` included implementation, update the `openPartStream()` overridden method to handle the error message to allow the return of established status code and respective message, based on chosen range.