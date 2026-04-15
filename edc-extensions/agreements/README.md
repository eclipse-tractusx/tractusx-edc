# Agreements Retirement Extension

This extension is introduced to allow a dataspace dataset provider to _prematurely_ retire an active contract agreement.

Contract agreements are immutable entities by design. 
The word prematurely is used here since contract agreements should only expire once the contractual terms agreed upon between participants no longer holds.

Even though the previous statements are valid, the need to prematurely retire an active contract agreement exists if, for example, 
the contract agreement is a digital representation of a physical agreement which might have changed via legal
mechanisms, hence resulting in a new contract. The digital representation is no longer valid and shouldn't allow any data transfers.

## Technical approach

Since contract agreements are immutable, the following approach was used to represent a contract agreement retirement.

A policy pre validator was introduced that checks if the attached contract agreement exists in the `AgreementRetirementStore`.
If it exists, the validation is considered failed.

This policy pre validator is registered both in the `policy-monitor` and `transfer` scopes. In both cases, a failed pre validation
leads to transfer process termination.

An API was created to enable dataset providers to manage `AgreementRetirementEntry` entities in the `AgreementRetirementStore` via an endpoint.

## AgreementRetirementEntry schema

An `AgreementRetirementEntry` is composed of:
- A contract agreement id.
- A retirement reason.
- An agreement retirement timestamp.

`AgreementRetirementEntry` entities can be managed via the `/retireagreements` endpoint of the data management API.
Please refer to the swagger API spec for detailed examples on how to manage these entities.

## Impact on active or new transfer processes

Once a contract agreement is retired, all active transfer processes related with that agreement
will be terminated. New transfer process requests made from the consumer using the retired agreement will also fail with an 
agreement is invalid message.