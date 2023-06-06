# Postgresql SQL Migration Extension

This extension applies SQL migrations to

* the asset-index
* the contract-definition store
* contract-negotiation store
* policy store
* transfer-process store

## Configuration

| Key                                                                       | Description                                      | Mandatory | Default  |
|:--------------------------------------------------------------------------|:-------------------------------------------------|-----------|----------|
| org.eclipse.tractusx.edc.postgresql.migration.asset.enabled               | Enable migration for asset tables                |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.contractdefinition.enabled  | Enable migration for contract definition tables  |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.contractnegotiation.enabled | Enable migration for contract negotiation tables |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.edr.enabled                 | Enable migration for edr tables                  |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.policy.enabled              | Enable migration for policy tables               |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.transferprocess.enabled     | Enable migration for transfer process tables     |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.schema                      | The DB schema to be used during migration        |           | "public" |
