# Postgresql SQL Migration Extension

This extension applies SQL migrations to these stores:

* dataplane
* accesstokendata

## Configuration

| Key                                                                       | Description                                      | Mandatory | Default  |
|:--------------------------------------------------------------------------|:-------------------------------------------------|-----------|----------|
| org.eclipse.tractusx.edc.postgresql.migration.dataplane.enabled           | Enable migration for data flow tables            |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.accesstokendata.enabled     | Enable migration for access token data tables    |           | true     |
| org.eclipse.tractusx.edc.postgresql.migration.schema                      | The DB schema to be used during migration        |           | "public" |
