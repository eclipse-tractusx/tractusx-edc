# EDR Core Extension

This extension provide a base implementation of `EdrManager` and `EdrService` both
required for interacting with the EDR APIs and state machine

The EDR state machine handle the lifecycle of a negotiated EDR. The negotiation request can be submitted
via EDR APIs, and it will go through two phases:

- Contract Negotiation
- Transfer Request

## EDR Core Configuration

The new API is seamlessly deployed under the well-known management API context, requiring no additional configuration.
However, this is not the case for the EDR Cache storage. The EDR Cache provides two implementations: `InMemory` and `SQL`.
The `edc-runtime-memory` bundle utilizes the `InMemory` implementation for the EDR Cache without any extra setup.
On the other hand, the SQL Cache, found in the `edc-controlplane-postgresql-*` bundles, utilizes the SQL implementation
and requires database access for schema migration and data management.

| property                                                  | description                                          | required | default value |
|-----------------------------------------------------------|------------------------------------------------------|----------|---------------|
| edc.datasource.edr.name                                   | Defines the name associated with the EDR data source | false    | edr           |
| edc.datasource.edr.url                                    | Defines the database address to access the EDR data  | true     |               |
| edc.datasource.edr.user                                   | Defines EDR database username                        | true     |               |
| edc.datasource.edr.password                               | Defines EDR database password                        | true     |               |

## EDR state machine Configuration

Once the Transfer Request has completed, the EDR entry will be saved with the associated EDR in the primordial state `NEGOTIATED`
The state machine will also manage the lifecycle and the renewal of the `EDR`. If a token is about to expire it will
transition to the `REFRESHING` state and fire off another transfer process with the same parameter of the expiring
one. Once completed the new `EDR` will be cached and the old ones, with same `assetId` and `agreementId` will transition
into the `EXPIRED` state. Then the state machine will also monitor the `EXPIRED` ones, and will delete them according to the
retention configuration.

| Key                                         | Description                                                                                         | Mandatory | Default |
|:--------------------------------------------|:----------------------------------------------------------------------------------------------------|-----------|---------|
| edc.edr.state-machine.iteration-wait-millis | The iteration wait time in milliseconds in the edr state machine                                    |           | 1000    |
| edc.edr.state-machine.batch-size            | The batch size in the edr negotiation state machine                                                 |           | 20      |
| edc.edr.state-machine.expiring-duration     | The minimum duration on which the EDR token can be eligible for renewal (seconds)                   |           | 60      |
| edc.edr.state-machine.expired-retention     | The minimum duration on with the EDR token can be eligible for deletion when it's expired (seconds) |           | 60      |
