# Proposal for FederatedCatalog with Tractus-X distribution

## Decision

The proposed solution is to have a Federated Catalog as own runnable service capable of crawling all the chosen catalogs and expose that data.

## Rationale

The Federated Catalog aims at providing the aggregation of catalogs from multiple participants in a centralized point to reduce latency.

Choosing a solution that decouples it from the Control Plane (like the one used for the Data Plane) and able to be scalable will future-proof the Federated Catalog as a feature and embraces wider usage.

## Approach

The Federated Catalog running as own instance can be performed either by running respective Docker image in a container or directly running a generated `jar`. 

> In case of aiming at testing locally, the [Readme.md](https://github.com/eclipse-edc/FederatedCatalog/blob/main/README.md) includes a very helpful guide. 

Regarding deployment, the Tractus-Connector Helm charts will be updated to include this service.  

Relevant to highlight some potential challenges of the proposed approach. 
- The Network Latency expected may be a concern, but not more than any other downstream dependencies of the service;
- Having its own instance (that itself contains a cache) may use considerable computing resources (related with storage cost).

To enable the Federated Catalog flow, please [see this table](https://github.com/eclipse-tractusx/tractusx-edc/blob/75bdacbad43e2cad352204ea28a359c6aac7adea/docs/development/management-domains/README.md#enable-and-configure-the-crawler-subsystem).