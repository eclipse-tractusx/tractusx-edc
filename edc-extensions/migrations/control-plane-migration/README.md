# Postgresql SQL Migration Extension

This extension applies SQL migrations to these stores:

* asset-index
* contract-definition
* contract-negotiation
* edr
* policy
* policy-monitor
* transfer-process

## Configuration

| Key                                                     | Description                                         | Mandatory | Default  |
|:--------------------------------------------------------|:----------------------------------------------------|-----------|----------|
| tx.edc.postgresql.migration.asset.enabled               | Enable migration for asset tables                   |           | true     |
| tx.edc.postgresql.migration.contractdefinition.enabled  | Enable migration for contract definition tables     |           | true     |
| tx.edc.postgresql.migration.contractnegotiation.enabled | Enable migration for contract negotiation tables    |           | true     |
| tx.edc.postgresql.migration.edr.enabled                 | Enable migration for edr tables                     |           | true     |
| tx.edc.postgresql.migration.policy.enabled              | Enable migration for policy tables                  |           | true     |
| tx.edc.postgresql.migration.policy-monitor.enabled      | Enable migration for policy monitor tables          |           | true     |
| tx.edc.postgresql.migration.transferprocess.enabled     | Enable migration for transfer process tables        |           | true     |
| tx.edc.postgresql.migration.agreementbpns.enabled       | Enable migration for contract agreement bpns tables |           | true     |
| tx.edc.postgresql.migration.agreementretirement.enabled | Enable migration for agreement retirement tables    |           | true     |
| tx.edc.postgresql.migration.bpn.enabled                 | Enable migration for business partner group tables  |           | true     |
| tx.edc.postgresql.migration.dataplaneinstance.enabled   | Enable migration for dataplane instance tables      |           | true     |
| tx.edc.postgresql.migration.jti-validation.enabled      | Enable migration for jti-validation tables          |           | true     |
| tx.edc.postgresql.migration.schema                      | The DB schema to be used during migration           |           | "public" |
