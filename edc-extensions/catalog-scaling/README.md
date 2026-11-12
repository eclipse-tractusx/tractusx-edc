# Catalog Scaling Extension

This extension provides an implementation for `DatasetResolver` that is designed to handle larger amounts of assets
and contract definitions when generating a catalog. While the upstream implementation is designed for a smaller number
of contract definitions, each selecting multiple assets, in practice assets and contract definitions are often created
in a 1:1 relation, which then causes very high load and memory consumption in the control plane runtime for a large
number of assets and contract definitions. The implementation provided by this extension moves the matching of assets
and contract definitions to the database with an index-backed fast-path for all criteria with operators `=` and `in`. As
`=` is commonly used when defining assets and contract definitions in a 1:1 relation, this allows for a significant
performance improvement when generating a catalog.

## Prerequisites

As the `DatasetResolver` implementation provided by this extension moves the matching of assets and contract definitions
to the database and relies heavily on Postgres features for doing so, it can only be used by a control plane
runtime using Postgres as its database.

> As the extension uses a `TRIGGER`, the Postgres version needs to be >= 14.

## How It Works

The `DatasetResolver` implementation provided by this extension uses a set of Postgres functions to run the queries
for finding and generating datasets. At start-up, the extension runs an
[SQL script](src/main/resources/sql/script.sql) that creates the necessary functions, as well as additional columns in
the asset and contract definition tables, which are managed by Postgres either as a generated column or through a
trigger.

For assets, the effective properties (i.e. the asset's properties merged over its private properties) are stored in
a separate column. This makes them GIN-indexable, which allows for faster queries when searching for assets with
specific properties. For contract definitions, the asset selector is split into multiple columns, making the later query
for matching contract definitions to assets faster.

### Process Steps

With the functions and additional columns, the process of generating datasets for a catalog request is as follows:

1. Resolve authorized contract definitions (same as upstream implementation)
2. Create a temporary table in Postgres with the same schema and defaults as the contract definition table
3. Copy authorized contract definitions to the temporary table
4. Run `ANALYZE` on the temporary table to update Postgres statistics for efficient query planning
5. Build the assets query as defined by the supplied QuerySpec incl. offset & limit, and using the `id` as a sort field to ensure stable ordering of results
6. Query for assets using the function `edc_find_matching_assets_page`, which returns matching assets incl. IDs of all matching contract definitions per asset
7. For each returned asset, build a dataset

A sequence diagram of this process is available in the [docs directory](docs/1_dataset-resolver-sequence.mmd).

### The Query Functions

Internally, the query function `edc_find_matching_assets_page` utilizes multiple smaller functions as well as indexes
to efficiently find matching assets and contract definitions.

First, the contract definitions' asset selectors are split into multiple columns, to separate criteria using the
operators `=` or `in` from other criteria. This split is done on every insert or update of a contract definition
through a trigger, so that the computation does not happen during a catalog request for the entire table at once. 
As `=` and `in` criteria can be expressed as JSON containment checks on the properties, this allows Postgres to filter
for assets matching these criteria using a GIN index, which greatly improves query performance. The remaining criteria,
if any are present, are then evaluated one-by-one.

For evaluation of the remaining criteria, a few functions are created which mirror the EDC classes responsible for
matching criteria against properties. They also consider EDC's property lookup precedence (path navigation first, then
whole key match & properties first, then private properties).

When a single dataset is requested instead of a catalog, the function `edc_find_authorized_asset` is used instead,
which first looks for the corresponding asset by ID, and then utilizes the same functions to find matching contract
definitions for the asset.

A component diagram of the query functions is available in the [docs directory](docs/2_postgres-functions.mmd).
You can find in-depth documentation for each function in the [SQL script](src/main/resources/sql/script.sql) itself.

## Configuration

This extension relies on the existing properties `edc.sql.store.asset.datasource` and
`edc.sql.store.contractdefinition.datasource` to identify the datasource to use, and takes one additional configuration
property:

`edc.sql.store.contractdefinition.copy.batch.size`: number of contract definition to copy to the temporary table in one batch. Default: `5000`

## Additional Notes

Some things to consider when using this extension are listed below.

### Known divergences from Java-based implementation

While the Postgres functions try to mirror EDC's matching logic as closely as possible, there are 2 divergences. These
only occur in very specific cases, but should be kept in mind.

1. The operators `like` and `ilike` are used to match a property against a regular expression. When the respective property
is a string, the result is the same. However, when the property is e.g. an object, the Java-based implementation will
perform the regex-matching on the `toString()` version of this object. As Postgres rendering of objects and arrays as
text differs from Java's `toString()`, the result will differ. This should not be an issue, as `like`/`ilike` are
typically used with strings, but should be kept in mind.
2. If no value is found for a given key in an asset's properties or private properties, the Java-based implementation will
fall back to fields of the asset itself (like e.g. `createdAt`), which is not done by the Postgres implementation.

### Table/row locks on start-up

As the added columns on the asset and contract definition tables are managed and filled by Postgres, the tables will
be locked while the values are computed and inserted. On an initial run, this can take a few seconds, but as the runtime
is still booting up and not yet serving requests, this should not cause any issues. On subsequent runs, e.g. when a
second pod for the runtime is started, the script will check if the columns already exist and skip their creation if
present, so no table lock will occur.

If however the column or trigger is changed or dropped, the script will re-create them, so the tables will be locked
during computation. As long as the columns or triggers are not modified or dropped manually, this case should not occur.

## NOTICE

This work is licensed under the CC-BY-4.0

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2026 Volkswagen AG
- Source URL: https://github.com/eclipse-tractusx/tractusx-edc
