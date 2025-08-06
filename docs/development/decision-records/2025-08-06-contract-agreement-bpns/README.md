# Persist provider and consumer BPNs for all contract agreements

## Decision

Introduce an extension that stores provider and consumer BPNs for each contract agreement in a dedicated database table.

## Rationale

In EDCs aligned with DSP 2025-1 and DCP v1.0, contract agreements reference participants by their DIDs, but BPN remains 
the legal identifier. To ensure compliance and traceability, we need to persist participant's BPNs alongside every agreement.

## Approach

1. Define new entity/model that captures the contract agreement ID, provider BPN, and consumer BPN.
2. Enable persistence for the new entity, including a database migration to create the corresponding table.
3. Implement an `EventSubscriber` for `ContractNegotiationFinalized` events that:
    1) If the contract agreement contains DIDs, map them to the respective BPNs
    2) Persists the agreement ID and associated BPNs in the new table.
4. Extend the `BdrsClient` to cache and retrieve BPNs by DID for efficient lookup.