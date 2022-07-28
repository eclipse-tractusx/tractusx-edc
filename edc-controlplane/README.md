# Control Plane

The Eclipse Dataspace Connector consists of a **Control Plan** and a **Data Plane** Application.
While the **Data Plane** handles the actual Data Transfer, the **Control Plane** is responsible for:

- Resource Management (e.g. Assets, Policies & Contract Definitions CRUD)
- Contract Offering & Contract Negotiation
- Data Transfer Coordination / Management

The only API that is protected by some kind of security mechanism is the Data Management API. At the time of writing this is done by a simple API key.
The key value must be configured in `edc.api.auth.key`. All requests to the Data Management API must have `X-Api-Key` header with the key value.

Example:
```bash
curl -X GET <URL> --header "X-Api-Key: <edc.api.auth.key>"
```

## Security

### Confidential Settings

Please be aware that there are several confidential settings, that should not be part of the actual EDC configuration file.

Some of these confidential settings are
- Vault credentials
- Data Management API key
- Database credentials

As it is possible to configure EDC settings via environment variables, one way to do it would be via Kubernetes Secrets. For other deployment scenarios than Kubernetes equivalent measures should be taken.

# Known Control Plane Issues

Please have a look at the open issues in the open source repository. The list below might not be maintained well and
only contains the most important issues.
EDC Github Repository https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues

---

**Please note** that some of these issues might already be fixed on the EDC main branch, but are not part of the specific
EDC commit the Product-EDC uses.

---

**Configuration**
- Contract negotiation not working when `web.http.ids.path` is configured/changed ([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1249))
  - **Workaround:** Don't configure `web.http.ids.path`, so that the default path is used.

**Data Management API**
- Contract negotiation not working when initiated with policy id ([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1251))
  - **Workaround:** The DataManagement API can also initiate a contract negotiation using the actual policy object.

- Contract-Offer-Receiving-Connectors must also pass the ContractPolicy of the ContractDefinition before receiving offers([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1331))

- Deletion of Asset becomes impossible when Contract Negotiation exists([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1403))
  - **Workaround:** Delete Asset from DB manually. Be aware that deleting Assets, that are part of a ContractNegotiation or ContractAgreement, may corrupt the connector instance!

- Deletion of Policy becomes impossible when Contract Definition exists([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1410))
  - **Workaround:** Delete Policy from DB manually. Be aware that deleting Policies, that are part of a ContractDefinition, ContractNegotiation or ContractAgreement, may corrupt the connector instance!

**Other**
- Non-IDS-Transformable-ContractDefinition causes connector to be unable to send out self-descriptions/catalogs([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1265))
  - **Workaround:** Delete non-transformable ContractDefinition or Policy.

**Security**
- DataAddress is passed unencrypted from DataProvider to DataConsumer ([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1504))
  - **Workaround:** Use only test data!
