# Observability API customization

This extension is a (temporary) replacement for - and a 1:1 stand-in for the EDC `observability-api` module. It exposes
the same endpoints as the upstream module, with one important distinction: users can configure in which HTTP context the
API gets registered, and whether or not insecure (= unauthenticated) access is allowed.

## Default behaviour

If no additional configuration is done, the Observability API is registered into the `"management"` context of EDC.
That means the following configuration values **must be present**

```
web.http.management.port=<PORT>
web.http.management.path=/some/api/path
```

Further, the ObservabilityApi is secured with the default `AuthenticationService`, most likely a token-based one.

## Allowing insecure access

If the Observability API should be unauthenticated, the following configuration is required:

```
tractusx.api.observability.allow-insecure=true
web.http.observability.port=<PORT>
web.http.observability.path=/some/api/path
```

If the `tractusx.api.observability.allow-insecure=true` is set, then the Observability API will get registered
into the `observability` context, which is unsecured.

> Disclaimer: allowing unsecured access to APIs is dangerous and a potential security risk! Using authenticated access
> to all APIs is highly recommended. Never expose unsecured APIs to the public! 