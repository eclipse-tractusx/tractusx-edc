# Dataplane to enable proxying datasource's original response

## Decision

The dataplane public API will optionally expose the original response from an HTTP data source based on a new Data type
in the source data address.

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. This was implemented as so to avoid
exposing any potential sensitive information to external parties.

However, there is a new requirement to allow the option of proxying the original response from the HTTP datasource back
to the consumer. The current implementation will be extended to optionally allow proxying of the datasource response
(successful and otherwise) and will only be applicable to HTTP Data Sources (for PULL transfer types) depending on a new
Data type added by the data provider to the http source data address.

The original consideration was to perform this change upstream, however the dataplane public API was marked as
deprecated.

## Approach

1. Pull the upstream modules `data-plane-util` and `data-plane-http-spi` into the Tractus-X EDC distribution.
2. A new `ProxyHttpData` type will be added to the `HttpDataAddress`. Additionally, a new  `ProxyHttpDataSource` will be
   created for this specific data type and a `ProxyHttpPart` will also be created to include the original `statusCode`.
   The existing `HttpData` will keep the existing behaviour, i.e., return only 2XX's and 5XX's.
3. Update the pulled `AsyncStreamingDataSink` to proxy the entire response from the source for this Data type,
   successful or not. This includes the status code, media type and content.

The following code snippet illustrates partly the changes added to the `ProxyHttpDataSource`. Overall, will be similar
to the `HttpDataSource`.

```java
private StreamResult<Stream<Part>> handleResponse(Response response) {
    // Omitted check response body here.
    var mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(OCTET_STREAM);
    var statusCode = (String.valueOf(response.code()));
    Stream<Part> content = Stream.of(new ProxyHttpPart(name, stream, mediaType, statusCode));
    return success(content);
}
```

4. Update the `DataPlanePublicApiV2Controller` to evaluate and return the proxied response for this new Data type. The
   callback will be updated to read from the original response, similar to the next. For the `HttpData` type, the
   existing behaviour will be maintained.

```java
        AsyncStreamingDataSink.AsyncResponseContext asyncResponseContext = callback -> {
    StreamingOutput output = t -> callback.outputStreamConsumer().accept(t);
    var resp = Response
            .status(retrieveStatusCode(callback.statusCode()))
            .entity(output)
            .type(callback.mediaType())
            .build();
    return response.resume(resp);
};
```