# Multi Tenancy

This extension provide support for a multi-tenant EDC.

## TL;DR
Please take a look at the [related sample](../../samples/sample-multi-tenancy)

## How to use
The module provides an extension of the `BaseRuntime` class called `MultiTenantRuntime`.
Setting this as the main class will make the application look for a properties file which path can be specified with
the `edc.tenants.path` property.

In this file the tenants are defined through settings, e.g.:
```properties
edc.tenants.tenant1.edc.fs.config=/config/path
edc.tenants.tenant2.edc.fs.config=/config/path
edc.tenants.tenant3.edc.fs.config=/config/path
```

Using this file the EDC will run with 3 tenants: `tenant1`, `tenant2` and `tenant3`, every one with their respective
configuration file.
Everything that stays after the tenant name in the setting key will be loaded in the tenant runtime, so *theoretically*
(but not recommended) you could define all the tenants configuration in the tenants properties file:
```properties
edc.tenants.tenant1.web.http.port=18181
edc.tenants.tenant1.any.other.setting=value
edc.tenants.tenant2.web.http.port=28181
edc.tenants.tenant3.web.http.port=38181
```
