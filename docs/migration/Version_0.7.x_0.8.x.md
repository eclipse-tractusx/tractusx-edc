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
