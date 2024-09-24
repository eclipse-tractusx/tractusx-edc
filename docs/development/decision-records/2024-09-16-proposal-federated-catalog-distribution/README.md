# Proposal for FederatedCatalog with Tractus-X distribution

## Decision

The **proposed solution is to have a Federated Catalog as own runnable service** capable of crawling all the wanted catalogs and expose that data.


## Rationale

The Federated Catalog aims at providing the aggregation of catalogs from multiple participants in a centralized point to reduce latency.

Choosing a solution that decouples from Control Plane (like the one used for the Data Plane) and able to be scalable will future-proof the Federated Catalog as a feature and embraces wider usage. 
Like so, having a Federated Catalog service, scalable on its own based on user needs, and not on Control Plane, allows the best resource management in the long term. It also enables adding features to the Federated Catalog without the need to change the Control Plane logic (assuming no API usage change).

Additionally, the Federated Catalog deployment flow can be very similar to EDC components, ensuring the consistency and no need for additional know-how to handle the CI.

Overall, aiming at independent upgrades and instance scalability, the decision above is more suitable when compared with the Federated Catalog embedded and shipped as any other extension in the ControlPlane.



## Approach

The Federated Catalog running as own instance can be performed either by running respective Docker image in a container or running a generated `jar`. Both approaches should be very straightforward 

> In case of aiming at testing locally, the [Readme.md](https://github.com/eclipse-edc/FederatedCatalog/blob/main/README.md) includes a very helpful guide for both approaches mentioned. 

Regarding infrastructure concerns, this service deployment approach can be very similar to the Data Plane, like a creation of a `deployment-federatedcatalog.yaml` very similar [to this one](https://github.com/eclipse-tractusx/tractusx-edc/blob/a263bf71a110245657131509d4b37d058a1d220d/charts/tractusx-connector-azure-vault/templates/deployment-dataplane.yaml#L47). Considering hosting and other user infrastructure requirements, once again, the Data Plane can be used as a baseline comparison.  

Relevant to highlight some potential challenges of the proposed approach. 
- The Network Latency expected may be a concern, but not more than any other downstream dependencies of the service;
- Having its own instance (that itself contains a cache) may use considerable computing resources (related with storage cost).

Finally, and also working as a reminder, to enable this flow in any usage, it must be enabled. [See this table](https://github.com/eclipse-tractusx/tractusx-edc/blob/75bdacbad43e2cad352204ea28a359c6aac7adea/docs/development/management-domains/README.md#enable-and-configure-the-crawler-subsystem) for additional information.