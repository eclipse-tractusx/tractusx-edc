# Tractus-X Contract Agreement Retirement

## Decision

Implement a mechanism that allows a dataset provider to _prematurely_ retire an active contract agreement, terminating all
related transfer processes and stopping new data transfer requests.

## Rational

The need to prematurely retire an active contract agreement exists if, for example,
the contract agreement is a digital representation of a physical agreement which might have changed via legal
mechanisms, hence resulting in a new contract. The digital representation is no longer valid and shouldn't allow any data transfers.

Contract agreements are immutable entities by design and should only expire once the contractual terms agreed upon between participants no longer holds.
Considering such restriction, the implemented solution should provide a mechanism that allows for contract agreement retirement
without changing the existing contract agreement feature.

## Approach

An `AgreementRetirementStore` will be introduced to persist `AgreementRetirementEntry` entities that represent contract agreement
retirements.

A policy function will be introduced that checks if the attached contract agreement exists in the `AgreementRetirementStore`.
If true, the evaluation is considered failed. This policy function shall be registered both in the `policy-monitor` and `transfer` scopes. 
In both cases, a failed policy evaluation leads to transfer process termination.

An API will also be introduced to enable dataset providers to manage `AgreementRetirementEntry` entities in the `AgreementRetirementStore`.

### AgreementRetirementEntry entity

A new entity will be created to represent an agreement retirement. This entity contains the following attributes:

- `agreementId` -> The id of the retired contract agreement.
- `reason` -> The reason why the contract agreement was retired.
- `agreementRetirementDate` -> A date (as timestamp) when the contract agreement was retired.