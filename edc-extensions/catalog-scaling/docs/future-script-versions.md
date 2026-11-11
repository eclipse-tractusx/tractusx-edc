# Updating functions, columns or indexes in future versions

The script runs at every start-up in a single transaction, possibly while other pods are serving requests against
the same database. Every version must therefore be re-runnable, must not lock tables when nothing changes, and must
keep working when an older version of the script runs afterwards (rollback, rolling deployment). In the following, some
points to consider are listed when the script is changed in future versions.

## Functions

Functions are created with `CREATE OR REPLACE`, so changing the body, language, volatility or comment in place is
possible. `CREATE OR REPLACE` will fail if the return type (including `RETURNS TABLE` columns), `OUT` parameters,
input parameter names or default values change. Such changes therefore require a `DROP` of the function first or
a new function name. `DROP` should be used with caution and only be executed once no deployed version uses the
respective function anymore. **Creating new versions of functions with a new name (e.g. `*_v2`) is therefore the
preferred way!**

> `edc_effective_properties` (used by a generated column) and `edc_contract_definitions_split_selector` (used by a
trigger) cannot be dropped without also dropping the generated column or the trigger.

## Stored Columns

- `edc_contract_definitions.selector_*` columns are maintained by a trigger. After `edc_split_selector` changes, the
  next run recomputes outdated rows. Until that run commits, these rows are locked and updates to them wait.
- `edc_asset.effective_properties` is a generated column. Its stored values are **not** recomputed when
  `edc_effective_properties` changes. A changed logic requires a new column with a new function and a new index; adding
  it rewrites `edc_asset` under an exclusive lock that blocks all access until commit.