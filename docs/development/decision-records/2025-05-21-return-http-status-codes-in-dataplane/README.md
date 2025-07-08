# Dataplane to expose selected HTTP status code, message and response body

## Decision

The dataplane API will expose the status codes, respective messages and response body based on a new proxy status code flag in the request parameters. 

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. However, there is a requirement to allow the option of returning different http status codes (like 4XX) back to the consumer with the respective message and response body.

To achieve it, a new `proxyStatusCodes` flag will be added to the http data address in which, if set to `true`, the dataplane will return the specific http status code, message and response body. The status codes could be of any successful or error type. To ensure the routing of entire data from the original response, a new ```ProxyStreamFailure``` will be added that extends the ```StreamFailure``` with the inclusion of media type, status code and content.
The current behaviour will be kept as the default one by defining the flag default value as `false` and the proxying of status codes (successful and otherwise) will only apply to HTTP Data Sources (for both PUSH and PULL transfer types).

## Approach

1. Include the logic needed from `data-plane-http` and `data-plane-http-spi` (like `HttpDataSource`, `HttpRequestParams` or `HttpDataAddress`) upstream modules in the Tractus-X EDC distribution. 
2. In the `HttpRequestParams` include the new optional parameter `proxyStatusCodes`. If is not set, the default behaviour (`false`) will be kept, i.e., return only 2XX's and 5XX's.
3. Create a new `ProxyStreamFailure` class that extends `StreamFailure` to include the `mediaType`, `statusCode` and `content`. This class will only be used to encapsulate the failure response when the `proxyStatusCodes` is enabled.
4. In the `HttpDataSource` included implementation, update the `openPartStream()` overridden method to handle the error message to allow the return of the "real" status code, message and response body, based on flag value.


Expected change will be applied on a handle failure method that will return a `ProxyStreamFailure` containing the exact status code, message and response body.
```java
private StreamResult<Stream<Part>> handleFailureResponse(Response response, String statusCode) {
    try {
        var body = response.body();
        String mediaType = null;
        InputStream stream = null;
        if (body != null) {
            mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(OCTET_STREAM);
            stream = body.byteStream();
        }
        var streamFailure = new ProxyStreamFailure(
                List.of(
                        response.message(),
                        format("Received code transferring HTTP data: %s - %s.", response.code(), response.message())),
                null,
                stream,
                mediaType,
                statusCode);
        return failure(streamFailure);
    } finally {
        try {
            response.close();
        } catch (Exception e) {
            monitor.severe("Error closing failed response", e);
        }
    }
}
```
The above snippet will only be applied to the proxy scenario, where the existing one will be maintained as it is.

5. Update the `DataPlanePublicApiV2Controller` to evaluate the failure and return the proxied status codes and respective messages.
