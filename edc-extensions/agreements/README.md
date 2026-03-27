# Agreements Retirement Extension
## Overview

The Agreements extension allows a **data provider** to **prematurely retire** an active contract agreement. Contract agreements are immutable in EDC; they normally expire only when the contractual terms no longer hold. This extension addresses cases where the digital agreement must be invalidated earlier—for example when a physical or legal agreement changes and the digital representation should no longer allow data transfers.

Retirement is implemented by storing retirement entries and failing policy validation when a transfer or policy-monitor flow uses a retired agreement. The extension provides a Management API to manage retirement entries and optional SQL persistence.

---

## Modules

### retirement-evaluation-core
Registers pre-validators on the Policy Engine for transfer and policy-monitor scopes -> `AgreementsRetirementServiceImpl`.
#### 1. Policy validation (pre-validator)
- **AgreementRetirementValidator** checks whether the contract agreement ID exists in the retirement store. If it does, validation **fails**.
- Registered as a **pre-validator** for:
    - **TransferProcessPolicyContext**
    - **PolicyMonitorContext**
- So: once an agreement is retired, **new** transfer requests using that agreement fail, and **existing** transfer processes can be terminated by the policy monitor when it re-evaluates.

### retirement-evaluation-api
Management API (v3) to create, list, and remove retirement entries.
#### 1. Management API (v3)
Base path: **`/v3/contractagreements/retirements`** (Management API context).

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/v3/contractagreements/retirements` | Retire an agreement (body: `AgreementsRetirementEntry`). |
| `POST` | `/v3/contractagreements/retirements/request` | List retired agreements (optional body: `QuerySpec`). Returns JSON array of retirement entries. |
| `DELETE` | `/v3/contractagreements/retirements/{agreementId}` | Reactivate: remove the retirement entry for the given agreement ID. |

**AgreementsRetirementEntry** (request/response):

- **agreementId**: Contract agreement ID to retire (required).
- **reason**: Retirement reason (required).
- **agreementRetirementDate**: Timestamp; defaulted to current time if omitted.


### retirement-evaluation-spi
SPI types and interfaces: `AgreementsRetirementEntry`, `AgreementsRetirementStore`, `AgreementsRetirementService`, and events (`ContractAgreementRetired`, `ContractAgreementReactivated`).
#### 1. Service and store
- **AgreementsRetirementService**: `isRetired(agreementId)`, `findAll(querySpec)`, `retireAgreement(entry)`, `reactivate(agreementId)`. Used by the validator and the API.
- **AgreementsRetirementStore**: `save(entry)`, `delete(contractAgreementId)`, `findRetiredAgreements(querySpec)`. Default implementation is **in-memory** (`InMemoryAgreementsRetirementStore`); replace with **retirement-evaluation-store-sql** for persistent storage.

#### 2. Events (SPI)
- **ContractAgreementRetired** / **ContractAgreementReactivated** — Emitted when an agreement is retired or reactivated (for downstream integration if needed).

### retirement-evaluation-store-sql
SQL persistence for `AgreementsRetirementStore` (Postgres-compatible schema). Optional; without it, the default in-memory store is used.

## How to Use in EDC
1. Minimum (in-memory store): Add core, API, and SPI to the control plane:
    ```kotlin
    implementation(project(":edc-extensions:agreements"))
    ```
   The core extension (pre-validators + default store), the API extension (REST) and the SPI are loaded.
   The default **DefaultAgreementRetirementStoreProviderExtension** provides an in-memory store.
2. Persistent store (SQL/PostgreSQL): Add the SQL store module so `AgreementsRetirementStore` is backed by the database:
    ```kotlin
    implementation(project(":edc-extensions:agreements:retirement-evaluation-store-sql"))
    ```
   Ensure the database is configured and the schema is applied (see `retirement-evaluation-store-sql/docs/schema.sql`: table `edc_agreement_retirement` with `contract_agreement_id`, `reason`, `agreement_retirement_date`).


## Impact on active or new transfer processes
Once a contract agreement is retired, all active transfer processes related with that agreement
will be terminated. New transfer process requests made from the consumer using the retired agreement will also fail with an 
agreement is invalid message.