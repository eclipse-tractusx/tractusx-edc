# Dataplane to enable proxying datasource's original response

## Decision

The dataplane public API will optionally expose the original response from an HTTP data source based on a new attribute
flag in the source data address.

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. This was implemented as so to avoid
exposing any potential sensitive information to external parties.
However, there is a new requirement to allow the option of proxying the original response from the HTTP datasource back
to the consumer. The current implementation will be extended to optionally allow proxying of the datasource response (
successful and otherwise) and will only be applicable to HTTP Data Sources (for PULL transfer types) depending on a new
flag added by the data provider to the http source data address.

The original consideration was to perform this change upstream, however the dataplane public API was marked as
deprecated.

## Approach

1. Pull the upstream modules `data-plane-http` and `data-plane-http-spi` into the Tractus-X EDC distribution.
2. A new `proxyOriginalResponse` flag will be added to the `HttpDataAddress`. When the value is set to `true`, the
   dataplane will return the original datasource response. During the instantiation of `HttpDataSource` the value is set
   to the parameter `proxyOriginalResponse`. If is not present in the `HttpDataAddress`, the default behaviour (`false`)
   will be kept, i.e., return only 2XX's and 5XX's.
3. Update the `HttpDataSource` to proxy the entire response from the source, successful or not, including the status
   code, media type and content. The proxied response will also contain the value of the new `proxyOriginalResponse`
   flag in a
   new `StreamResult` implementation that extends the one from upstream. This flag will then be evaluated in the last
   step of the request processing.

The following code snippet illustrates the changes made to the `HttpDataSource`.

```java
private StreamResult<Stream<Part>> handleSuccessfulResponse(Response response) {
    // Response body validation will be kept here, just removed to reduce the noise.
    var mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(OCTET_STREAM);
    var statusCode = (String.valueOf(response.code()));
    return success(
            Stream.of(new HttpPart(name, stream, mediaType)),
            mediaType,
            statusCode,
            params.isProxyOriginalResponseEnabled());
}

private StreamResult<Stream<Part>> handleFailureResponse(Response response, String statusCode) {
    var body = response.body();
    String mediaType = null;
    Stream<Part> content = null;
    if (body != null) {
        mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).get();
        var stream = body.byteStream();
        content = Stream.of(new HttpPart(name, stream, mediaType));
    }
    return failure(
            content,
            mediaType,
            statusCode,
            new StreamFailure(List.of(format("Received code transferring HTTP data: %s - %s.",
                    response.code(), response.message())), null),
            params.isProxyOriginalResponseEnabled());
}
```

The ```success()``` and ```failure()``` methods will be included in the new `StreamResult` implementation.

5. Update the `DataPlanePublicApiV2Controller` to evaluate and return the proxied response. For a successful scenario,
   an additional step will be created to evaluate the response's status code and media type and inject those values in
   the
   callback.