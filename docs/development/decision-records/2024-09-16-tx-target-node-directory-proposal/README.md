# Proposal for a TargetNodeDirectory in Tractus-X

## Decision

A service containing the data of desired BPNL's and respective Connectors' URL's that a certain partner wants, but here the partner itself has the service in their scope and users are able to input (in the API of new service) the Participants. Simply, the new service would have a db/cache containing the BPNL's and Connectors' URL's of each partner this member wants the offers from. Again, **this new service works as the TargetNodeDirectory for the partner's FederatedCatalog**.


## Rationale

While considering new interventions in the Federated Catalog, this discussion aims to propose a solution for the TargetNodeDirectory.
From the [documentation](https://eclipse-edc.github.io/docs/#/submodule/FederatedCatalog/docs/developer/architecture/federated-catalog.architecture)
> The Federated Catalog requires a list of Target Catalog Nodes, so it knows which endpoints to crawl. This list is provided by the TargetNodeDirectory. During the preparation of a crawl run, the ExecutionManager queries that directory and obtains the list of TCNs.


Using the Discovery Service, the FC is able to request the connetors' url's that it requires, providing the BPNL's. Since the Discovery Service has already built in this feature, the work of this flow is already relatively well established.
However, the main question here arises: what if a new partner joins the space? The existing partners that want to obtain the offers of the new partner do not know of its existence. So an additional mechanism is needed. The default implementation is a static file, however a dynamic approach would be preferable (no downtime expected).

Even with a new service registering the Discovery Service (basically making it available in the space to other partners) the existing partners that may want to retrieve the new partners' offers must establish the need to retrieve them by defining the new BPNL somewhere.



## Approach

### Proposed solution
#### Service per participant providing the specific needed data

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
-> Add a new BPNL
-> Remove a BPNL
-> List BPNL's (value and connectors associated with it)


Regarding the service's Persistence Store, any suggestion is welcomed. Having a DB or a local Cache can suffice, but other options like persisting in a file could also be considered. Or a mixed between this options could be the best solution.

ℹ️  Each partner must have the BPNL or the Connectors' URL's beforehand. If a new Partner registers and an existing partner would want their catalog, the BPNL or Connectors' URL's of the new partner must be obtain first.

### Open Questions

Q1: Would it make sense to have the catalog data for participants in the new service cache/db as well or should they be kept separate?

Q2: The idea would be to have an instance of the new service per EDC or one for partner? Our suggestion is to have an instance per partner, containing all the catalogs the multiple EDC's in that partner's space may need since all use the same BPN. This would result in FC's having data regarding the entire catalog that that partner needs despite some EDC only needing partial information. Still, having a single instance could simplify the overall approach and would partner would still only retain data they want.

## Other solutions considered
#### A file in a S3 bucket (or different cloud provider's solution)

Simply put, having a json file in a centralized point where all connectors could have access and update it if and when needed.

Cons:
- One file for all instead of each partner having the data that respectively needs does not match the requirement;
- This solution would lock the usage of a proprietary tool (cloud provider) and harder to sustain in an open source community. The idea would be to have a solution not "glued" to an external tool.


#### Centralized service with all participants data

A service could be created to contain all data regarding partners (participants in the space) and provide it to any allowed member in the space. In this scenario, the FC would consume through this service's API the data of explicitly requested partners. This new service works as the TargetNodeDirectory for the partner's FederatedCatalog.


Cons:
- Single point of failure, if service goes down, the FC in all partners becomes unavailable as well if new partners are registered in the space.
- Partner still needs to have a way to dynamically store on desired partners ids', so this solution is not complete.
- Each partner must have the BPNL or the Connectors' URL's beforehand. If a new Partner registers and an existing partner would want their catalog, the BPNL or Connectors' URL's of the new partner must be obtain first.

