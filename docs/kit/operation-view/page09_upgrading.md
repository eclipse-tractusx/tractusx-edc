# Upgrading TractusX EDC

Among the goals of TractusX EDC is making EDC upgrades as painless as possible.
The changes in each release are documented [here](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/docs/migration).
Usually there are only two steps to each upgrade.

## Database Migration

Database migration is simple to accomplish with a PostgreSQL backend.
The [PostgreSQL Extension](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/postgresql-migration) is the preferred approach.
Alternatively, the `.sql` files therein can be used to manually update the database schema.

## Updating EDC

The easy part of the upgrade process is to simply switch the outdated EDC containers with their newer counterparts.
