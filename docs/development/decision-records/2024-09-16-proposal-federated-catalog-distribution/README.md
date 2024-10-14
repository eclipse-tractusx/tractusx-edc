# Proposal for FederatedCatalog with Tractus-X distribution and its TargetNodeDirectory

## Decision

The Federated Catalog will be deployed as a standalone component. The Tractus-X EDC Connector Helm charts will be updated to feature a new Federated Catalog deployment template.
Regarding the TargetNodeDirectory, a new extension in the FederatedCatalog will have a db/cache containing the BPNL's and Connectors' URL's of each partner a member wants the offers from. The member defines the BPNL's through a new API exposed by the extension.

## Rationale

While a standalone component (= K8S deployment) brings a slight increase in configuration complexity, its ability to be managed and scaled independently makes up for that.

For TargetNodeDirectory it will be set by a new extension responsible for exposing an API, where a member can input the BPNL's of the participants from which the catalogs are wanted, and then it will retrieve and store the respective Connector URL's. This new extension would get the data from the Discovery Service, provided a BPNL, and will be named `DiscoveryServiceRetrieverExtension`. This solution allows the member to choose precisely the Target Catalog Nodes that interests them, resulting in reduced network calls and latency.
Additionally, if a Connector URL is registered (or unregistered) in the Discovery Service, the retriever will reflect it since it requests based on BPNL (which should not change) and the registered URL's will be returned.

This solution improves on the default one of having the data in a static file since a dynamic approach would avoid downtime when a change is required.

Other solution for the TargetNodeDirectory was also considered
- File in a S3 bucket (or different cloud provider's solution)
  - This solution was discarded due to one file for all instead of each partner having the data that respectively needs does not match the requirement and this solution would lock the usage of a proprietary tool (cloud provider) being harder to sustain in the long run.


## Approach

Since the Federated Catalog will be a standalone runtime, the Tractus-X EDC Connector Helm charts will be updated to include the Federated Catalog as a separated deployment. The update will include the creation of a specific `deployment-federatedcatalog.yaml`, similar [to this one](https://github.com/eclipse-tractusx/tractusx-edc/blob/a263bf71a110245657131509d4b37d058a1d220d/charts/tractusx-connector-azure-vault/templates/deployment-dataplane.yaml#L47) (for `ingress` and `hpa` as well), for different scenarios (InMemory, PostreSQL, etc.). This results in added configuration complexity.

To enable the Federated Catalog flow, please [see this table](https://github.com/eclipse-tractusx/tractusx-edc/blob/75bdacbad43e2cad352204ea28a359c6aac7adea/docs/development/management-domains/README.md#enable-and-configure-the-crawler-subsystem).

For its TargetNodeDirectory, the user is able to obtain the Connectors' URL's through the Discovery Service and store them in the new extension through the new extensions' API. The API will allow to save a list of BPNLs (and Connectors' URL's if desired) and the `DiscoveryServiceRetrieverExtension` is responsible to retrieve the data and store it (in memory or in a database). The URL's can later be retrieved and crawled by the Federated Catalog.
If no BPNL is saved (to be crawled) then the extension will not request data from the Discovery Service, since the number of catalogs in the space can be significant.

The retrieval of Connector URL's through the Discovery Service is enabled by the endpoint:
```
POST: /api/administration/connectors/discovery
```
In which, the body of the request can contain the BPNL's related with participants from which the catalogs want to be obtained. Although the DiscoveryService allows to perform a request without providing BPNL's (empty list) it will not be done by the extension.
Information regarding the related API can be found [here](https://catenax-ev.github.io/docs/standards/CX-0001-EDCDiscoveryAPI#22-api-specification).

Some limitations of this TargetNodeDirectory solution are:
- Each partner must have the BPNLs beforehand. If a new Partner is registered and an existing partner would want their catalog, the BPNL (or Connector URL's) of the new partner must be obtained first and added through the new extension API;
- Deal with the overhead an additional persistence store;
- The usage of the Discovery Service requires a technical user account to access it (must be requested). After obtaining them, the credentials can be stored in the vault.


As indicated, the new extension would have own API capable of:

#### Save BPNL's
A member can add a BPNL through this API from which the Connector URL's are needed. This extension will iterate over the listed BPNL's and query the Discovery Service.
Request body would contain a list of BPNL's, allowing to store in bulk.
```
[POST] /api/target-nodes
```
Request Body Example
```json
[ "BPNL000000000001","BPNL000000000002" ]
```

#### Remove a stored BPNL
Once a member understands that they do not need the Catalogs from a certain BPNL, this can be removed.
BPNL to be remvoed is sent as a path param.
```
[DELETE] /api/target-nodes/{bpnl}
```
#### Retrieve BPNL's
Get BPNL's (value and connectors associated with it).
```
[POST] /api/target-nodes/request
```
Request Body Example
```json
[ "BPNL000000000001","BPNL000000000002" ]
```
Response Example
```json
[
    {
        "bpn": "BPNL000000000001",
        "connectorEndpoint": [
            "https://connector1/api/v1/dsp"
        ]
    },
    {
        "bpn": "BPNL000000000002",
        "connectorEndpoint": [
            "https://connector2/api/v1/dsp",
            "https://connector3/api/v1/dsp",
            "https://connector4/api/v1/dsp"
        ]
    }
]
```