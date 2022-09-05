# Release Notes Version 0.1.1
31.08.2022


> **BREAKING CHANGES**
> 
> Please consolidate the migration documentation ([link](../migration/Version_0.1.0_0.1.1.md)).

## 0. Summary

- 1. Eclipse Dataspace Connector Update
- 2. New Extensions
  - 2.1 CX IAM OAuth2 Extension
- 3. Bug Fixes

## 1. Eclipse Dataspace Connector Update

Due to problems with the EDC release pipeline this repository will _again_ build the artifacts agin using Git submodule.

The Git submodule references a commit, older than **0.0.1-milestone-6**.

## 2. New Extensions

The following extensions are now included in the base image of the connector.

### 2.1 CX IAM OAuth2 Extension

Using the open source OAuth Extension it is possible for a connector to re-use an IDS DAPS Token and forge the own identity (replay attack). To mitigate the security issue for the upcoming release Catena-X introduces its own OAuth2 IAM Extension. Except for the audience, the IAM configuration stays similar.

[Documentation](../../edc-extensions/cx-oauth2/README.md)


**New Audience Configuration**

```
edc.ids.endpoint.audience=http://plato-edc-controlplane:8282/api/v1/ids/data
```

## 3. Bug Fixes

This section covers the most relevant bug fixes, included in this version.

- Connectors using the Azure Key Vault could not start ([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1892))
