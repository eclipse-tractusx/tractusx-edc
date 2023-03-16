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
EDC Github Repository https://github.com/eclipse-edc/Connector/issues

---

**Please note** that some of these issues might already be fixed on the EDC main branch, but are not part of the specific
EDC commit the Product-EDC uses.

---

**Persistence**
- ContractDefinition-AssetSelector of InMemory Connector selects 50 Asset max.([issue](https://github.com/eclipse-edc/Connector/issues/1779))

**Other**
- Non-IDS-Transformable-ContractDefinition causes connector to be unable to send out self-descriptions/catalogs([issue](https://github.com/eclipse-edc/Connector/issues/1265))
  - **Workaround:** Delete non-transformable ContractDefinition or Policy.
