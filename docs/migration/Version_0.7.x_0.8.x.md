# Migration Guide `0.7.x -> 0.8.x`

Note: this guide is still work in progress, if you have some additional feedback to give, please use the related discussion:
https://github.com/eclipse-tractusx/tractusx-edc/discussions/1579

<!-- TOC -->
* [Migration Guide `0.7.x -> 0.8.x`](#migration-guide-07x---08x)
  * [1. New datasource configuration](#1-new-datasource-configuration)
  * [2. New SQL schema migrations](#2-new-sql-schema-migrations-)
    * [2.1. Control plane:](#21-control-plane)
    * [2.2. Data plane:](#22-data-plane)
  * [3. Deprecation cleanup](#3-deprecation-cleanup)
  * [4. New Contract Agreements Retirement Feature](#4-new-contract-agreements-retirement-feature)
  * [5. Application program arguments](#5-application-program-arguments)
    * [5.1. Changing Log Level via TractusX helm charts](#51-changing-log-level-via-tractusx-helm-charts)
<!-- TOC -->

## 1. New datasource configuration

So far the SQL datasource configuration was 1:1 with the "store context" one, so the EDC forced to use a dedicated 
datasource for every store, now the two concepts are separated, so by default there will be a single `default` datasource
defined in the Helm Chart and by default all the stores will use that.

So, if you are not using the provided [Tractus-X Helm Charts](https://eclipse-tractusx.github.io/charts/) and you don't
need to use different datasources for different stores, we suggest to remove all the `edc.datasource.<xxx>` settings and
leave just the `edc.datasource.default` settings group. If you use the Tractus-X EDC Helm Charts there is no further action required on your part.

In any case please follow the advice given by the `WARNING` log messages in the EDC console output.

Ref. https://eclipse-edc.github.io/documentation/for-contributors/postgres-persistence/

## 2. New SQL schema migrations 
For those who are not using either the Helm Charts or the provided [`migration` modules](../../edc-extensions/migrations):

### 2.1. Control plane:
- [contract negotiation- create state index](../../edc-extensions/migrations/control-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/contractnegotiation/V0_0_9__Alter_ContractNegotiation_CreateStateIndex.sql)
- [data plane instance - init schema](../../edc-extensions/migrations/control-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/dataplaneinstance/V0_0_1__Init_Dataplaneinstance.sql)
- [federated catalog - init schema](../../edc-extensions/migrations/control-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/federatedcatalog/V0_0_1__Init_FederatedCatalogCache_Database_Schema.sql)
- [policy monitor - create state index](../../edc-extensions/migrations/control-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/policy-monitor/V0_0_2__Alter_PolicyMonitor_CreateStateIndex.sql)
- [policy definition - add profiles column](../../edc-extensions/migrations/control-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/policy/V0_0_5__Add_Profiles.sql)
- [transfer process - create state index](../../edc-extensions/migrations/control-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/transferprocess/V0_0_16__Alter_TransferProcess_CreateStateIndex.sql)

### 2.2. Data plane:
- [dataplane - add transfer type destination column](../../edc-extensions/migrations/data-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/dataplane/V0_0_2__Alter_Dataplane_AddTransferTypeDestinationColumn.sql)
- [dataplane - create state index](../../edc-extensions/migrations/data-plane-migration/src/main/resources/org/eclipse/tractusx/edc/postgresql/migration/dataplane/V0_0_3__Alter_Dataplane_CreateStateIndex.sql)

## 3. Deprecation cleanup

There were many deprecations deleted after some time, mostly regarding `spi`s, something about configuration settings.
**Management API didn't change**, we're still using upstream version 3.

If you noticed and took care of everything that was logged as `WARNING` (which we strongly recommend) you shouldn't find
any other difficulties.

## 4. New Contract Agreements Retirement Feature

A new extension was added that allows a dataspace participant to prematurely retire an active contract agreement.
Once a contract agreement is retired, all existing transfer processes will be terminated. New transfer process requests,
using the retired contract agreement, will also fail to start.

The extension adds a set of API endpoints that allow the dataspace participant to manage its retired contract
agreements.
API details can be found in the OpenAPI specification.

Further details about the extension can be found in the extension [README](../../edc-extensions/agreements/README.md)
file.

## 5. Application program arguments

When using the distributed tractusx-edc docker images, you can now specify program arguments that will change how the
Connector behaves. Example:

`docker run edc-runtime-memory:0.8.0 --no-color`

The supported program arguments and their behavior are:

- Removing the coloring option of the default Console Monitor logs by using the program argument `--no-color`.
- Changing the Connectors' default Console Monitor log levels by using the program argument `--log-level=WARNING`.
  Possible log levels are
  `[SEVERE, WARNING, INFO, DEBUG]`.

### 5.1. Changing Log Level via TractusX helm charts

If you use the TractusX helm charts, you can change the log level of the default Console Monitor by changing the
`logs.level` configuration in `values.yml` of you chart (example below). The default level is set as `DEBUG` by default.

https://github.com/eclipse-tractusx/tractusx-edc/blob/03df535b822f7e6430032feb3775280e123b03cc/charts/tractusx-connector-memory/values.yaml#L77-L79

The old log level configurations were removed (below), as they were not used by the connector in any way and could
induce the user
of the helm charts in error.

```diff
#-- configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html)
-logging: |-
-.level=INFO
-org.eclipse.edc.level=ALL
-handlers=java.util.logging.ConsoleHandler
-java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
-java.util.logging.ConsoleHandler.level=ALL
-java.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n
```