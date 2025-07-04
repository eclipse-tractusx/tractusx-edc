# Dataplane to expose selected HTTP status code, message and response body

## Decision

The dataplane API will expose the status codes, respective messages and response body based on a new proxy status code flag in the request parameters. 

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. However, there is a requirement to allow the option of returning different http status codes (like 4XX) back to the consumer with the respective message and response body.

To achieve it, a new `proxyStatusCodes` flag will be added to the http data address in which, if set to `true`, the dataplane will return the specific http status code, message and response body. All proxied responses will be of successful status since the proxy itself is successful, regardless of original response, with the proxy response containing the original response data.
The current behaviour will be kept as the default one by defining the flag default value as `false` and the proxying of status codes as successful will only apply to HTTP Data Sources (for both PUSH and PULL transfer types).

## Approach

1. Include the logic needed from `data-plane-http` and `data-plane-http-spi` (like `HttpDataSource`, `HttpRequestParams` or `HttpDataAddress`) upstream modules in the Tractus-X EDC distribution. 
2. In the `HttpRequestParams` include the new optional parameter `proxyStatusCodes`. If is not set, the default behaviour (`false`) will be kept, i.e., return only 2XX's and 5XX's.
3. In the `HttpDataSource` included implementation, update the `openPartStream()` overridden method to handle the message to allow the return of the "real" status code, message and response body (based on flag value) inside the proxied response.

Expected change will be applied on a handle proxy method that. The proxy response will be of successful status with the respective content containing the original status code, message and response body.
```java
private StreamResult<Stream<Part>> handleProxyResponse(Response response) {
    var body = response.body();
    var statusCode = response.code();
    if (body == null) {
        throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s", requestId, statusCode));
    }

    var stream = body.byteStream();
    responseBodyStream.set(new ResponseBodyStream(body, stream));
    var mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(OCTET_STREAM);
    var message = response.message();

    monitor.debug(format("Received proxyable HTTP data: %s - %s.", statusCode, message));

    return success(Stream.of(new HttpProxyablePart(name, stream, mediaType, statusCode, message)));
}
```
