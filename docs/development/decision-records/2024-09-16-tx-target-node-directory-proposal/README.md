# Proposal for a TargetNodeDirectory in Tractus-X

## Decision

A new extension in the FederatedCatalog that will have a db/cache containing the BPNL's and Connectors' URL's of each partner a member wants the offers from, acting as the TargetNodeDirectory for the partner's FederatedCatalog.


## Rationale

While considering new interventions in the Federated Catalog, this decision aims to set the TargetNodeDirectory.
From the [documentation](https://github.com/eclipse-edc/FederatedCatalog/blob/e733355c6991ff633ee009bd5f35ced61e941da6/docs/developer/architecture/federated-catalog.architecture.md)
> The Federated Catalog requires a list of Target Catalog Nodes, so it knows which endpoints to crawl. This list is provided by the TargetNodeDirectory. During the preparation of a crawl run, the ExecutionManager queries that directory and obtains the list of TCNs.

The goal is the creation of an extension responsible for exposing an API, where a member can input the BPNL's of the participants from which the catalogs are wanted, and then it will retrieve and store the respective Connector URL's. This new extension would get the data from the Discovery Service, provided a BPNL, and will be named `DiscoveryServiceRetrieverExtension`. However, each member has control to retrieve the Connector URL's from a different source, just needing to create an extension.
This solution allows the member to choose precisely the Target Catalog Nodes that interests them, resulting in reduced network calls and latency.  

Additionally, if a Connector URL is registered (or unregistered) in the Discovery Service, the retriever will reflect it since it requests based on BPNL (which should not change) and the registered URL's will be returned.

Other solution was also considered

- File in a S3 bucket (or different cloud provider's solution)
    - This solution was discarded due to one file for all instead of each partner having the data that respectively needs does not match the requirement and this solution would lock the usage of a proprietary tool (cloud provider) being harder to sustain in the long run.

## Approach

The user is able to obtain the Connectors' URL's through the Discovery Service and store them in the new extension through its API. The API will allow to save a list of BPNLs (and Connectors' URL's if desired) and the `DiscoveryServiceRetrieverExtension` is responsible to retrieve the data and store it (in memory or in a database). The URL's can later be retrieved and crawled by the Federated Catalog.

This solution improves on the default one of having the data in a static file since a dynamic approach would avoid downtime when a change is required.

Some imitations of this solution are:
- Each partner must have the BPNLs beforehand. If a new Partner registers and an existing partner would want their catalog, the BPNL (or Connector URL's) of the new partner must be obtained first;
- Deal with the overhead an additional persistence store;
- The usage of the Discovery Service requires a technical user account to access it (must be requested).