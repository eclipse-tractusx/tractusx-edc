# Dataplane to expose selected HTTP status code and respective message

## Decision

The dataplane API will expose the status codes and respective messages based on a new proxy status code flag in the request parameters. 

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. However, there is a requirement to allow the option of returning different http status codes (like 4XX) back to the consumer with the respective message.

To achieve it, a new `proxyStatusCodes` flag will be added to the http data address in which, if set to `true`, the dataplane will return the specific http status code and respective message. The status codes could be of any successful or error type.
The current behaviour will be kept as the default one by defining the flag default value as `false`.

## Approach

1. Include the logic needed from `data-plane-http` and `data-plane-http-spi` (like `HttpDataSource`, `HttpRequestParams` or `HttpDataAddress`) upstream modules in the Tractus-X EDC distribution. 
2. In the `HttpRequestParams` include the new optional parameter `proxyStatusCodes`. If is not set, the default behaviour (`false`) will be kept, i.e., return only 2XX's and 5XX's.
3. In the `HttpDataSource` included implementation, update the `openPartStream()` overridden method to handle the error message to allow the return of the "real" status code and respective message, based on flag value.
4. Update the `DataPlanePublicApiV2Controller` to return the proxied status codes and respective messages.