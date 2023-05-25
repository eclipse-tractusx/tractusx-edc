# SQL-based `EndpointDataReferenceCache`  extension

This extensions provide a persistent implementation of `EndpointDataReferenceCache`.

It will store in the database this fields:

- tranferProcessId
- agreementId
- assetId
- edrId

It represent a single EDR negotiation done with the new Control Plane Adapter APIs.

The EDR itself it is stored in the participant vault with a prefixed key `edr__<edrId>`.

**_Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may
work but might have unexpected side effects!_**

## 1. Table schema

see [schema.sql](docs/schema.sql).

## 2. Configuration

| Key                                    | Description                       | Mandatory | Default |
|:---------------------------------------|:----------------------------------|-----------|---------|
| edc.datasource.edr.name                | Datasource used by this extension |           | edr     |
