# Proposal for a TargetNodeDirectory in Tractus-X

## Decision

A new service that will have a db/cache containing the BPNL's and Connectors' URL's of each partner a member wants the offers from. Simply, **this new service works as the TargetNodeDirectory for the partner's FederatedCatalog**.


## Rationale

While considering new interventions in the Federated Catalog, this discussion aims to propose a solution for the TargetNodeDirectory.
From the [documentation](https://eclipse-edc.github.io/docs/#/submodule/FederatedCatalog/docs/developer/architecture/federated-catalog.architecture)
> The Federated Catalog requires a list of Target Catalog Nodes, so it knows which endpoints to crawl. This list is provided by the TargetNodeDirectory. During the preparation of a crawl run, the ExecutionManager queries that directory and obtains the list of TCNs.

So having a service containing the data of desired BPNL's and respective Connectors' URL's that a certain partner wants, but here the partner itself has the service in their scope and users are able to input (in the API of new service) the Participants.

This solution allows the member to choose the relevant data that interests them (offers from specific participants) enabling the reduction of storage cost and less network calls. Additionally, each member has control on how to host and manage this new service, service changes do not affect other parties (unless contract changes) and is able to be independently scalable.


Other solutions were also considered

- File in a S3 bucket (or different cloud provider's solution)
    - This solution was discarded due to One file for all instead of each partner having the data that respectively needs does not match the requirement and this solution would lock the usage of a proprietary tool (cloud provider) being harder to sustain in an open source community.

- Centralized service with all participants data
    - Solution was not considered due to presenting a single point of failure, the partner still needs to have a way to dynamically store on desired partners ids' and each partner must have the BPNL or the Connectors' URL's beforehand.

## Approach

Using the Discovery Service, the FC is able to request the connetors' url's that it requires, providing the BPNL's. Since the Discovery Service has already built in this feature, the work of this flow is already relatively well established.
However, the main question here arises: what if a new partner joins the space? The existing partners that want to obtain the offers of the new partner do not know of its existence. So an additional mechanism is needed. The default implementation is a static file, however a dynamic approach would be preferable (no downtime expected).

Even with a new service registering the Discovery Service (basically making it available in the space to other partners) the existing partners that may want to retrieve the new partners' offers must establish the need to retrieve them by defining the new BPNL somewhere.


A service containing the data of desired BPNL's and respective Connectors' URL's that a certain partner wants, but here the partner itself has the service in their scope and users are able to input (in the API of new service) the Participants. Simply, the new service would have a db/cache containing the BPNL's and Connectors' URL's of each partner this member wants the offers from. Again, **this new service works as the TargetNodeDirectory for the partner's FederatedCatalog**.


Expected flow

- Using the new service API, partner adds the participants that wants to obtain the offers from (BPNL's);
- New service stores this data (db or cache);
- Providing the BPNL, the FC obtains the Connectors' URL's from the DS API;
- Using the new service API, FC passes the info from the DS to the new service;
- New service stores this data (db or cache);
- FC, with the ExecutionManager, periodically crawls the participants catalogs based on which ones are stored in the new service;
- FC catalog cache is updated;
- If new participant joins the space, user adds that participant's BPNL through new service API (like first step)

![tnd_proposal](https://github.com/user-attachments/assets/5245f8af-845d-4707-8871-359457d581e3)


⚠️  Any case that the Discovery Service (DS) is used, **a technical user account to access it must be requested**. Without it, the usage of the DS API is limited since a token is used in all relevant requests.

Technically, the new service **would have a simple API** where the user could
- Add a new BPNL
- Remove a BPNL
- List BPNL's (value and connectors associated with it)


Regarding the service's Persistence Store, any suggestion is welcomed. Having a DB or a local Cache can suffice, but other options like persisting in a file could also be considered. Or a mixed between this options could be the best solution.

ℹ️  Each partner must have the BPNL or the Connectors' URL's beforehand. If a new Partner registers and an existing partner would want their catalog, the BPNL or Connectors' URL's of the new partner must be obtain first.

Finally, considering service deployment, a new chart can be created just for this new service (similar to the existing ones), being its usage only decided by the member. As so, a Dockerfile should exist to ease this approach (giving the user option of running it in a container or run a simple `jar`).

The clear limitation of this approach is the overhead of a new service, specially one with a persistence store. This will increase costs in favour of faster responses.