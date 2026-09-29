# End to End testing persistence

## Decision

We will stop to test both in memory and postgresql persistence in e2e tests


## Rationale

The Tractus-X EDC main artifacts are the PostgreSQL backed ones, while the "in-memory" one (`edc-runtime-memory`) is
only meant for samples and local testing, as it cannot in fact be used in a proper production environment, so
continuously running long and hard to debug e2e tests to check in-memory persistence does not provide any additional
value. 
In memory stores will be tested using the `*TestBase` classes as we always did. 

Plus, the usage of in-memory tests in that scope can lead developers to forget to implement/apply proper changes to the
PostgreSQL implementation (as already happened in the past.)


## Approach

- Provide PostgreSQL persistence to all the e2e runtimes.
- Remove `runtime-memory` runtime
- Stop using the `@PostgresqlIntegrationTest` annotation in the e2e tests and use `@EndToEnd` instead
