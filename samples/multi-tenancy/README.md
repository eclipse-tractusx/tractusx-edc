# Multi Tenancy

This sample show how to create a custom runtime to run multiple EDC tenants in a single java process.

## How it works

In a Java Runtime, multiple "sub-runtimes" with dedicated classloader can be launched in parallel, giving object-level
separation (an object instantiated by a sub-runtime cannot be accessed by another sub-runtime).

## How to use

The module provides an extension of the `BaseRuntime` class called `MultiTenantRuntime`.
This class can be set in the `build.gradle.kts` as the main class:

```kotlin
application {
    mainClass.set("org.eclipse.tractusx.edc.samples.multitenancy.MultiTenantRuntime")
}
```

This runtime looks for a properties file which path can be specified with the `edc.tenants.path` property.

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

## Sample

Build:

```shell
./gradlew :samples:multi-tenancy:build
```

Run:

```shell
java -jar -Dedc.tenants.path=samples/multi-tenancy/tenants.properties samples/multi-tenancy/build/libs/multitenant.jar
```

Create a PolicyDefinition on `first` tenant:

```shell
curl -X POST http://localhost:18183/management/v3/policydefinitions \
    --header 'Content-Type: application/json' \
    --data '{
                "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                "policy": {
                    "@context": "http://www.w3.org/ns/odrl.jsonld",
                    "@type": "set",
                    "permission": [],
                    "prohibition": [],
                    "obligation": []
                  }
                }
                '
```

Get `first` tenant policy definitions:

```shell
curl -X POST http://localhost:18183/management/v3/policydefinitions/request
```

Will get a list containing the PolicyDefinition we created:

```json
[
  {
    "@id": "f48f2e27-c385-4846-b8b8-112c08bfa424",
    "@type": "edc:PolicyDefinition",
    "edc:createdAt": 1691147860257,
    "edc:policy": {
      "@id": "898fa3d6-b488-4f5f-9a41-4fb4b9229813",
      "@type": "odrl:Set",
      "odrl:permission": [],
      "odrl:prohibition": [],
      "odrl:obligation": []
    },
    "@context": {
      "dct": "https://purl.org/dc/terms/",
      "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
      "edc": "https://w3id.org/edc/v0.0.1/ns/",
      "dcat": "https://www.w3.org/ns/dcat/",
      "odrl": "http://www.w3.org/ns/odrl/2/",
      "dspace": "https://w3id.org/dspace/v0.8/"
    }
  }
]
```

`second` and `third` tenants will have no policy definitions:

```shell
curl -X POST http://localhost:28183/management/v3/policydefinitions/request
```

and

```shell
curl -X POST http://localhost:38183/management/v3/policydefinitions/request
```

will return

```json
[]
```
