# Proposal for FederatedCatalog with Tractus-X distribution

## Decision

Federated Catalog will be deployable as a standalone component capable of crawling all the chosen catalogs and expose that data. The Tractus-Connector Helm charts will be updated to feature a new Federated Catalog deployment template.

## Rationale

Choosing a solution that decouples it from the Control Plane (like the one used for the Data Plane) and able to be scalable will future-proof the Federated Catalog as a feature and embraces wider usage.

Having a specific runtime incurs on additional overhead (new Helm Chart, as example) and results in additional configuration complexity. Also, periodical crawling results in increased remote calls over time. However, being decoupled from the Control Plane, this solution permits the Federated Catalog to scale independently (based on own demand).  

## Approach

Since the Federated Catalog will be a standalone runtime, the Tractus-Connector Helm charts will be updated to include the Federated Catalog as a separated deployment. The update will include the creation of a specific `deployment-federatedcatalog.yaml`, similar [to this one](https://github.com/eclipse-tractusx/tractusx-edc/blob/a263bf71a110245657131509d4b37d058a1d220d/charts/tractusx-connector-azure-vault/templates/deployment-dataplane.yaml#L47) (for `ingress` and `hpa` as well), for different scenarios (InMemory, PostreSQL, etc.). This results in added configuration complexity.


To enable the Federated Catalog flow, please [see this table](https://github.com/eclipse-tractusx/tractusx-edc/blob/75bdacbad43e2cad352204ea28a359c6aac7adea/docs/development/management-domains/README.md#enable-and-configure-the-crawler-subsystem).