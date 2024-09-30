# Proposal for FederatedCatalog with Tractus-X distribution and its TargetNodeDirectory

## Decision

Federated Catalog will be deployable as a standalone component capable of crawling all the chosen catalogs and expose that data. The Tractus-Connector Helm charts will be updated to feature a new Federated Catalog deployment template.
Regarding the TargetNodeDirectory, a new extension in the FederatedCatalog will have a db/cache containing the BPNL's and Connectors' URL's of each partner a member wants the offers from.

## Rationale

Considering the Federated Catalog distribution, choosing a solution that decouples it from the Control Plane (like the one used for the Data Plane) and able to be scalable will future-proof the Federated Catalog as a feature and embraces wider usage.
Having a specific runtime incurs on additional overhead (new Helm Chart, as example) and results in additional configuration complexity. Also, periodical crawling results in increased remote calls over time. However, being decoupled from the Control Plane, this solution permits the Federated Catalog to scale independently (based on own demand).

For TargetNodeDirectory it will be set by a new extension responsible for exposing an API, where a member can input the BPNL's of the participants from which the catalogs are wanted, and then it will retrieve and store the respective Connector URL's. This new extension would get the data from the Discovery Service, provided a BPNL, and will be named `DiscoveryServiceRetrieverExtension`. This solution allows the member to choose precisely the Target Catalog Nodes that interests them, resulting in reduced network calls and latency.
Additionally, if a Connector URL is registered (or unregistered) in the Discovery Service, the retriever will reflect it since it requests based on BPNL (which should not change) and the registered URL's will be returned.

Other solution for the TargetNodeDirectory was also considered
- File in a S3 bucket (or different cloud provider's solution)
  - This solution was discarded due to one file for all instead of each partner having the data that respectively needs does not match the requirement and this solution would lock the usage of a proprietary tool (cloud provider) being harder to sustain in the long run.


## Approach

Since the Federated Catalog will be a standalone runtime, the Tractus-Connector Helm charts will be updated to include the Federated Catalog as a separated deployment. The update will include the creation of a specific `deployment-federatedcatalog.yaml`, similar [to this one](https://github.com/eclipse-tractusx/tractusx-edc/blob/a263bf71a110245657131509d4b37d058a1d220d/charts/tractusx-connector-azure-vault/templates/deployment-dataplane.yaml#L47) (for `ingress` and `hpa` as well), for different scenarios (InMemory, PostreSQL, etc.). This results in added configuration complexity.

To enable the Federated Catalog flow, please [see this table](https://github.com/eclipse-tractusx/tractusx-edc/blob/75bdacbad43e2cad352204ea28a359c6aac7adea/docs/development/management-domains/README.md#enable-and-configure-the-crawler-subsystem).

For its TargetNodeDirectory, the user is able to obtain the Connectors' URL's through the Discovery Service and store them in the new extension through its API. The API will allow to save a list of BPNLs (and Connectors' URL's if desired) and the `DiscoveryServiceRetrieverExtension` is responsible to retrieve the data and store it (in memory or in a database). The URL's can later be retrieved and crawled by the Federated Catalog.
This solution improves on the default one of having the data in a static file since a dynamic approach would avoid downtime when a change is required.

Some limitations of this TargetNodeDirectory solution are:
- Each partner must have the BPNLs beforehand. If a new Partner registers and an existing partner would want their catalog, the BPNL (or Connector URL's) of the new partner must be obtained first;
- Deal with the overhead an additional persistence store;
- The usage of the Discovery Service requires a technical user account to access it (must be requested).