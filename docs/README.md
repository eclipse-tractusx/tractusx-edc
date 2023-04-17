# Tractus-X EDC

The Tractus-X EDC repository creates runnable applications out of EDC extensions from
the [Eclipse DataSpace Connector](https://github.com/eclipse-edc/Connector) repository.

When running a EDC connector from the Tractus-X EDC repository there are three setups to choose from. They only vary by
using different extensions for

- Resolving of Connector-Identities
- Persistence of the Control-Plane-State
- Persistence of Secrets (Vault)

## Connector Setup

The three supported setups are.

- Setup 1: Pure in Memory **Not intended for production use!**
  - [Control Plane](../edc-controlplane/edc-runtime-memory/README.md)
    - [IDS DAPS Extensions](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/iam/oauth2/daps)
    - In Memory Persistence done by using no extension
    - In Memory Keyvault with seedable secrets.
  - [Data Plane](../edc-dataplane/edc-dataplane-azure-vault/README.md)
    - [Azure Key Vault Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/vault/azure-vault)
- Setup 2: PostgreSQL & Azure Vault
  - [Control Plane](../edc-controlplane/edc-controlplane-postgresql/README.md)
    - [IDS DAPS Extensions](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/iam/oauth2/daps)
    - [PostgreSQL Persistence Extensions](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/store/sql)
    - [Azure Key Vault Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/vault/azure-vault)
  - [Data Plane](../edc-dataplane/edc-dataplane-azure-vault/README.md)
    - [Azure Key Vault Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/vault/azure-vault)
- Setup 3: PostgreSQL & HashiCorp Vault
  - [Control Plane](../edc-controlplane/edc-controlplane-postgresql-hashicorp-vault/README.md)
    - [IDS DAPS Extensions](https://github.com/eclipse-edc/Connector/tree/main/extensions/common/iam/oauth2/daps)
    - [PostgreSQL Persistence Extensions](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/store/sql)
    - [HashiCorp Vault Extension](../edc-extensions/hashicorp-vault/README.md)
  - [Data Plane](../edc-dataplane/edc-dataplane-hashicorp-vault/README.md)
    - [HashiCorp Vault Extension](../edc-extensions/hashicorp-vault/README.md)

## Recommended Documentation

### This Repository

- [Update EDC Version from 0.0.x - 0.1.x](migration/Version_0.0.x_0.1.x.md)
- [Application: Control Plane](../edc-controlplane)
- [Application: Data Plane](../edc-dataplane)
- [Extension: Business Partner Numbers](../edc-extensions/business-partner-validation/README.md)
- [Example: Local TXDC Setup](samples/Local%20TXDC%20Setup.md)
- [Example: Data Transfer](samples/Transfer%20Data.md)

### Eclipse Dataspace Connector

- [EDC Domain Model](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/architecture/domain-model.md)
- [EDC Open API Spec](https://github.com/eclipse-edc/Connector/blob/main/resources/openapi/openapi.yaml)
- [HTTP Receiver Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/http-receiver)
