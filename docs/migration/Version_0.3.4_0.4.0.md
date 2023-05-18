# Migration from 0.3.3 to 0.3.4

## Switching to DSP

The Eclipse Dataspace Connector protocol recently moved its protocol implementation from IDS to DSP as of
version `0.0.1-milestone-9`.
From the Tractus-X EDC perspective this causes breaking changes in the following areas:

- the Management API: because DSP uses JSON-LD, all Management API endpoints had to be adapted as well to reflect that.
  The old Management API is now deprecated and is **not** tested for compliance. Please upgrade using the `/v2/` path
  for every endpoint, e.g. `<PATH>/management/v2/assets`. Please also refer to
  the [EDC OpenAPI spec](https://app.swaggerhub.com/apis/eclipse-edc-bot/management-api/0.0.1-SNAPSHOT#/).
- modules: all `*ids*` modules are deprecated and cannot be used anymore. Please migrate over
  to `org.eclipse.edc:dsp:0.0.1-milestone-9`.

**Please note that this is not a complete documentation of the DSP Protocol, please refer to
the [official documentation](https://docs.internationaldataspaces.org/dataspace-protocol/overview/readme)**

## Removal of the Business Tests

The business tests were removed from the code base, because all the ever tested is already by other tests, specifically
the JUnit-based tests, deployment tests, or other tests that are already done upstream in EDC.

The Business tests were brittle, consumed a lot of resources and were quite cumbersome to run and debug locally.

## New implementation for the Control Plane Adapter

Since the old Control-Plane-Adapter is incompatible with DSP, a new iteration was created.
**Due to time constraints with this release documentation for this feature will to be published subsequently**
