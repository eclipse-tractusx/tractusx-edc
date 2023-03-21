# Migration from 0.3.0 to 0.3.1

## Observability API changes

All endpoints of the Observability API now support unauthenticated access, if configured. That will put the
Observability API under a new context named `"observability"`, which consequently requires proper web context
configuration for it. Note that the name of the context cannot be changed.

## Settings changes

- `tractusx.api.observability.allow-insecure`: boolean value that enables (`true`) the unauthenticated access.
- `web.http.observability.port`: integer value that specifies the port of the `observability` context. **Mandatory if
  unauthenticated access is enabled!**
- `web.http.observability.path`: string value that specifies the path of the `observability` context. **Mandatory if
  unauthenticated access is enabled!**
