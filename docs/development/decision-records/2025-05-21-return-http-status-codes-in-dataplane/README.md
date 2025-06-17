# Dataplane to expose selected HTTP status code, message and response body

## Decision

The dataplane API will expose the status codes, respective messages and response body based on a new proxy status code flag in the request parameters. 

## Rationale

Currently, the dataplane does not return status code different from 2XX or 5XX. However, there is a requirement to allow the option of returning different http status codes (like 4XX) back to the consumer with the respective message and response body.

To achieve it, a new `proxyStatusCodes` flag will be added to the http data address in which, if set to `true`, the dataplane will return the specific http status code, message and response body. The status codes could be of any successful or error type.
The current behaviour will be kept as the default one by defining the flag default value as `false` and the proxying of status codes (successful and otherwise) will only apply to HTTP Data Sources (for both PUSH and PULL transfer types).

## Approach

1. Include the logic needed from `data-plane-http` and `data-plane-http-spi` (like `HttpDataSource`, `HttpRequestParams` or `HttpDataAddress`) upstream modules in the Tractus-X EDC distribution. 
2. In the `HttpRequestParams` include the new optional parameter `proxyStatusCodes`. If is not set, the default behaviour (`false`) will be kept, i.e., return only 2XX's and 5XX's.
3. In the `HttpDataSource` included implementation, update the `openPartStream()` overridden method to handle the error message to allow the return of the "real" status code, message and response body, based on flag value.

Expected change will be applied on an handle failure method that includes a validation of the new flag. If to be proxyed (i.e., flag is set to `true`) there will be returned a `StreamFailure` containing the exact status code, message and response body.
```java
    private StreamResult<Stream<Part>> handleFailureResponse(Response response, boolean containsProxyStatusCode) {
        try {
            if (NOT_AUTHORIZED == response.code() || FORBIDDEN == response.code()) {
                return StreamResult.notAuthorized();
            } else if (NOT_FOUND == response.code()) {
                return StreamResult.notFound();
            } else if (containsProxyStatusCode) {
                var streamFailure = new StreamFailure(
                        List.of(
                                String.valueOf(response.code()),
                                format("Received code transferring HTTP data: %s - %s - %S.", response.code(), response.message(), response.body())),
                        null);
                return failure(streamFailure);
            } else {
                return error(format("Received code transferring HTTP data: %s - %s - %S.", response.code(), response.message(), response.body()));
            }
        } finally {
            try {
                response.close();
            } catch (Exception e) {
                monitor.severe("Error closing failed response", e);
            }
        }
    }
```

4. Update the `DataPlanePublicApiV2Controller` to return the proxied status codes and respective messages.

```java
if (result.failed()) {
    var status = result.reason() == null
        ? Response.Status.fromStatusCode(Integer.parseInt(result.getFailureMessages().get(0)))
        : INTERNAL_SERVER_ERROR;
    response.resume(error(status, result.getFailureMessages()));
}
```