# Proposal for a TargetNodeDirectory in Tractus-X

## Decision

A new service that will contain Connectors' URL's of each partner a member wants the offers from, acting as the **TargetNodeDirectory for the partner's FederatedCatalog**.


## Rationale

While considering new interventions in the Federated Catalog, this decision aims to set the TargetNodeDirectory.
From the [documentation](https://eclipse-edc.github.io/docs/#/submodule/FederatedCatalog/docs/developer/architecture/federated-catalog.architecture)
> The Federated Catalog requires a list of Target Catalog Nodes, so it knows which endpoints to crawl. This list is provided by the TargetNodeDirectory. During the preparation of a crawl run, the ExecutionManager queries that directory and obtains the list of TCNs.

So having a service containing the data of Connectors' URL's that a certain partner wants and allow them to host it. Users are able to input (using the API of new service) the Connectors' URL's of the connectors they want the catalogs from.

This solution allows the member to choose precisely the Target Catalog Nodes that interests them, resulting in reduced network calls and latency. Additionally, each member has control on how to host and manage this new service. Service changes do not affect other parties (unless contract changes) and is able to be independently scalable.

Other solution was also considered

- File in a S3 bucket (or different cloud provider's solution)
    - This solution was discarded due to one file for all instead of each partner having the data that respectively needs does not match the requirement and this solution would lock the usage of a proprietary tool (cloud provider) being harder to sustain in the long run.

## Approach

The user is able to obtain the Connectors' URL's (through the Discovery Service, as an example) and store them in the new service through its API. The API will allow to save a list of Connectors' URL's in bulk. These can later be retrieved and crawled by the Federated Catalog.


Finally, considering service deployment, a new chart can be created just for this new service (similar to the existing ones), being its usage only decided by the member. As so, a Dockerfile should exist to ease this approach (giving the user option of running it in a container or run a simple `jar`).

Limitations of this solution are that each partner must have the Connectors' URL's beforehand and deal with the overhead of a new service, specially one with a persistence store.