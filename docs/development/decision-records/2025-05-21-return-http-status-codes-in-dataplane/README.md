# Dataplane to enable proxying datasource's original response

## Decision

The dataplane API will expose the original response from a datasource based on a new proxy status flag in the request
parameters.

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. This was implemented as so to avoid
exposing any potential sensitive information to external parties.
However, there is a new requirement to allow the option of proxying the original response from the datasource back to
the consumer. So the current implementation will be extended to allow the proxying of the datasource response (
successful and otherwise) and will only apply to HTTP Data Sources (for PULL transfer types) depending on a new flag
added to the http data address. The current behaviour will be kept as the default one.

The original consideration was to perform this change in Upstream, however the proxy dataplane was marked as deprecated.

## Approach

1. Pull implementations available in the upstream modules `data-plane-http` and `data-plane-http-spi` (
   like `HttpDataSource`, `HttpRequestParams` or `HttpDataAddress`) into the Tractus-X EDC distribution and update them
   to meet the new requirements.
2. A new `proxyOriginalResponse` flag will be added to the `HttpDataAddress`. When the value is set to `true`, the
   dataplane will return the original datasource response. In the `HttpRequestParams` include the new optional
   parameter `proxyOriginalResponse` flag. If is not set, the default behaviour (`false`) will be kept, i.e., return
   only 2XX's and 5XX's. This flag will then be evaluated in ```HttpDataSource.openPartStream()```.
3. For a failing datasource response, a new ```ProxyStreamFailure``` will be added (that extends
   the ```StreamFailure```) which includes the original response. This will include the `mediaType`, `statusCode`
   and `content`. This class will only be used to encapsulate the failure response when the `proxyOriginalResponse` is
   enabled and is needed since current approach does not allow to return a Failure message with "dynamic" status code,
   media type and content. For a successful response, the existing `StreamResult` can be used.
4. In the `HttpDataSource` included implementation, update the `openPartStream()` overridden method to handle the
   datasource response based on flag value.

Expected change will be applied on a handle failure method that will return a `ProxyStreamFailure` containing the exact
status code, message and response body.

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
                        format("Received code transferring HTTP data: %s - %s.", statusCode, response.message())),
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

For the successful response, the existing logic still applies, in the sense that media type and content are already
extracted from the response while a `StreamResult<Stream<Part>>` is returned.

5. Update the `DataPlanePublicApiV2Controller` to evaluate and return the proxied response.