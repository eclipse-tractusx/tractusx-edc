# Upgrading Tractus-X EDC

Among the goals of Tractus-X EDC is making EDC upgrades as painless as possible.
The changes in each release are documented [here](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/docs/migration).
Usually there are only three steps to each upgrade.

## Database Migration

Database migration is simple to accomplish with a PostgreSQL backend.
The [PostgreSQL Migration Extension](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/postgresql-migration) is the preferred approach.
Alternatively, the `.sql` files therein can be used to manually update the database schema.

## Updating EDC

The easy part of the upgrade process is to simply switch the outdated EDC containers with their newer counterparts.

## Updating Settings

Check the newest [Migration Documents](https://github.com/eclipse-tractusx/tractusx-edc/tree/develop/docs/migration)
for any changes to the settings structure and apply them to your settings.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
