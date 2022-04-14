# Control Plane

The Eclipse Dataspace Connector consists of a **Control Plan** and a **Data Plane** Application.
While the **Data Plane** handles the actual Data Transfer, the **Control Plane** is responsible for:

- Resource Management (e.g. Assets, Policies & Contract Definitions CRUD)
- Contract Offering & Contract Negotiation
- Data Transfer Coordination / Management

# Control Plane Setup

This chapter is about integration the Control Plane with the Azure KeyVault and IDS DAPS.

## Azure Key Vault Setup

The Eclipse Dataspace Connector requires a key vault, where it can store and retrieve secrets and certificates. </br>
At the time of writing the only key vault, the EDC is supporting, is the Azure Key vault.

### 1. Register a new App

In the Azure Portal:

1. Open **App registrations** page and create a new app
2. Choose a unique name and click _register_
3. The new App has a **Client ID** (also called Application ID). This ID must be configured in the connector
   setting `edc.vault.clientid`

For further information have a look at the official documentation </br>
https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app

### 2. Create App Secret

In the Azure Portal:

1. Open the page of the newly created app
2. On the left side select _certificates & secrets_
3. Create a new _client secret_
4. Add the secret value to the connector setting `edc.vault.clientsecret`

For further information have a look at the official documentation </br>
https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app#add-credentials

### 3. Create Azure Key Vault

In the Azure Portal:

1. Open **Key vaults** page and create a new Azure Key Vault
2. Fill out the mandatory fields, choose a unique key vault name and click _review + create_
3. The chosen name must be configured in the connector setting `edc.vault.name`
4. The directory ID of the key vault (also called tenant ID) must be configured in the `edc.vault.tenantid`

For further information have a look at the official documentation </br>
https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app

### 4. Provide the newly created App access to the Key Vault

In the Azure Portal:

1. Open the page of the newly created key vault
2. On the left side select _access policies_
3. Create new _access policy_ and select the appropriate permissions
5. Under _select principal_ add the newly created app from step 1
6. Click _add_

For further information have a look at the official documentation </br>
https://docs.microsoft.com/en-us/azure/key-vault/general/assign-access-policy?tabs=azure-portal

### 5. Summary

The complete Azure Key Vault configuration in the EDC should look something like this

```properties
edc.vault.tenantid=<aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa>
edc.vault.clientid=<bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb>
edc.vault.clientsecret=<ccccc~ccccccccc-cccccccccccccccccc>
edc.vault.name=<ddddd>
```

Please note that the key vault could also be configured using the `edc.vault.certificate`, which is not covered by this
documentation.

## Connector Certificate Setup

The connector needs it's own certificate / private key in the key vault, so that it is able to encrypt and decrypt data.
Therefore, generate a PEM file that contains the private key and the certificate and put it into the key vault.

```bash
# Generate PKCS8 Key
openssl genpkey -out con_key.pem -algorithm RSA -pkeyopt rsa_keygen_bits:2048

# Generate Certificate
openssl req -config cert.conf -new -x509 -key con_key.pem -nodes -days 365 -out con_cert.pem

# Create Cert+Key PEM file
cat con_cert.pem >certkey.pem
cat con_key.pem >>certkey.pem
```

Then in the Azure Portal:

1. Open the page of the newly created key vault
2. On the left side select _certificate_ and click _generate/import_
3. Select certificate creation method _import_, choose a unique name and upload _certkey.pem_ file into the value
4. The certificate name must be configured in the `edc.public.key.alias`

## IDS DAPS Setup

The Eclipse Dataspace Connector is able to retrieve an identity token from the IDS DAPS. This token is part of all IDS
messages.

The DAPS application requires a certificate from the Eclipse Dataspace Connector. This certificate may then be used by
the EDC connector to prove its identity and retrieve its identity token.

When writing this guidance these step were tested out using the open source omejdn DAPS of the Fraunhofer
AISEC ([GitHub](https://github.com/International-Data-Spaces-Association/omejdn-daps)).

### 1. Key / Certificate Generation

In the first step generate a PKSC8 Key and the corresponding certificate.

```bash
# Private Key
openssl genpkey -out key.pem -algorithm RSA -pkeyopt rsa_keygen_bits:2048
```

````bash
# Certificate
openssl req -new -x509 -key key.pem -nodes -days 365 -out cert.pem
````

### 2. Certificate Setup

#### 2.1. Certificate Setup - DAPS

Each connector client must be registered in the DAPS. Send the certificate to the DAPS maintainers. After the DAPS has
created a new client, configure the corresponding client ID in `edc.oauth.client.id`.

#### 2.2 Certificate Setup - Azure Key Vault

##### 2.2.1 Format Certificate

The certificate must also be stored as _secret_ in the Azure Key Vault. But before that, all newline and space
characters must be removed.

```bash
# Format Certificate
cat cert.pem | sed "s/[[:space:]]*//" | tr -d \\n >cert.txt
```

#### 2.2.2 Create Key Vault Secret

In the Azure Portal:

1. Open the page of the newly created key vault
2. On the left side select _secret_ and click _generate/import_ to create a new secret
3. Select upload options _manual_, choose a unique name and copy the content of the _cert.txt_ file into the value
4. The secret name must be configured in the `edc.oauth.public.key.alias`

### 3. Private Key Setup

#### 3.1 Format Key

Before storing the key as _secret_ in the Azure Key Vault format it. The PCKS8 identifying lines must be removed with
all spaces and newlines, so that only the key itself remains.

```bash
# Format Key
cat key.pem | sed "s/^-----BEGIN PRIVATE KEY-----//" | sed "s/-----END PRIVATE KEY-----$//" | sed "s/[[:space:]]*//" | tr -d \\n >key.txt
```

#### 3.2 Create Key Vault Secret

In the Azure Portal:

1. Open the page of the newly created key vault
2. On the left side select _secret_ and click _generate/import_ to create a new secret
3. Select upload options _manual_, choose a unique name and copy the content of the _key.txt_ file into the value.
4. The secret name must be configured in the `edc.oauth.private.key.alias`

### 4. DAPS Setup/Configuration

The these properties should be requested from the DAPS maintainer:

- DAPS Token URL must be configured in `edc.oauth.token.url`
- DAPS JWKS URL must be configured in `edc.oauth.provider.jwks.url`
- Token Audience must be configured in `edc.oauth.provider.audience`

### Summary

The EDC DAPS configuration could look like this:

```properties
edc.oauth.token.url=http://localhost:4567/token
edc.oauth.client.id=<clientId>
edc.oauth.provider.audience=<audience>
edc.oauth.provider.jwks.url=http://localhost:4567/.well-known/jwks.json
```

And for the OAUTH extensions there should the following properties set:

```properties
edc.oauth.public.key.alias=<az-kv-cert-name>
edc.oauth.private.key.alias=<az-kv-key-name>
```

# Short Overview of the EDC Domain

This chapter gives a short overview of the EDC domain. The idea is to get a basic understanding of the domain objects and their roles.</br>
The complete EDC documentation can be found in the official open source [EDC GitHub Repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

----

**Please note**
</br>
If you have already used the Fraunhofer Dataspace Connector, you are probably familiar with the IDS Domain Model. Don't confuse the IDS Domain Model with the EDC Domain Model.
The terms that are used in both models are pretty similar, but often don't represent the same thing. In the context of the EDC documentation it’s always safe to assume that the EDC Domain Model is in place, as the IDS model is only used when two Eclipse Dataspace COnnectors are exchanging messages.

----

## Contract Offer Exchange

In the EDC it’s not possible to create a _ContractOffer_ directly. The _ContractOffer_ is generated on the fly when
another connector asks about the _ContractOfferCatalog_. A _ContractOffer_ will only ge persistent when it becomes part of a
_ContractNegotiation_.

A _ContractDefinition_ defines how many _ContractOffers_ should be generated and how the _Policy_ should
look like.

The _ContractDefinition_ consists of a

- _ContractPolicy_, that describes in EDC ODRL terms how the policy for a _ContractOffer_ should look like.
- _AccessPolicy_, that describes in EDC ODRL terms who is able to see a _ContractOffer_. But the content of this policy
  will not be part of the _ContractOffer_.
- _AssetSelector_, that defines for which _Assets_ a _ContractOffer_ should be generated. An _Asset_ describes the data
  itself that may be offered/transferred and is comparable to the IDS triplet of IDS-Resource, IDS-Representation, IDS
  Artifact.

So the ContractDefinition looks somewhat like this:

<!--
Original PlantUML to update the ContractDefinition
---
@startuml

class ContractDefinition {
+String id
+Policy accessPolicy
+Policy contractPolicy
+AssetSelectorExpression selectorExpression
}
@enduml
-->

![test](http://www.plantuml.com/plantuml/png/PSvD2a9130FWVKyn-tU99-fUU2SOEbKAOqUQ28fuTnL_DYxpGK9ci2RFnowYlG9bFO9PbHlRUpXzHBd9j30z3iMRJBlHNQ-bgXhm3Z_KJ_dBAy2uM3VboEtbb0Qy5l57SXUHsQ8zhpm0)

When another connector asks the EDC about its _ContractOffers_ it:

- Checks the connector identity
- Finds all the _ContractDefinitions_ that have a passing AccessPolicy
- Finds for each passing _ContractDefinition_ the corresponding _Assets_
- Generates a new _ContractOffer_ for each _Asset_. The policy of the _ContractOffer_ is described in the ContractPolicy of the _ContractDefinition_.
- Maps the content of the _ContractOffer_ into the IDS domain and sends an IDS-ContractOffers to the other connector.
- The other connector then maps the IDS-ContractOffers back into its EDC domain and can then processes the _ContractOffer_.

# Data Management API

The documentation of the Data Management API can be found in the official open
source [EDC GitHub Repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

The complete Eclipse Dataspace Connector API is described in the [EDC Open API Specification](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/blob/main/resources/openapi/openapi.yaml). Please be aware that this specification contains all APIs, that are implemented in the open source repository. The extensions, that implement those APIs, might not be part of the Control- and/or Data-Plane applications in this repository. Additionally, depending on the extension configuration, the documented paths might be only reachable using the configured ports.

## Contract Offer Exchange

----

**Please note**</br>
This chapter showcases the contract offer exchange between two connectors. It should function as starting point when working the Eclipse Dataspace Connector API. For a more detailed explanation of the various topics, that are touched in this section, please consolidate the official documentation in the [EDC GitHub Repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

----

As described in the chapter about the EDC domain, the following resources must be created at the data provider:

- **Asset** (& DataAddress), describing the data and how it can be transferred
- **Policy**, as Contract- and/or AccessPolicy of the _ContractDefinition_
- **ContractDefinition**, for the contract offer generation

### 0. Calling the Data Management API

The Data Management API is secured with an API key. The value of this key can be configured in `edc.api.auth.key` and
should then be passed in the header as `X-API-Key: <api-auth-key>`.
Additionally, most or all of the API methods accept only JSON content, therefore adding `Content-Type: application/json`
to the header for most of the calls is recommended.

### 1. Create Asset using Data Mgmt API

#### Bash Script

```bash
# Variables (please update before running the script)
__connectorUrl=http://localhost:8181
__dataMgmtPath=data-mgmt
__apiKey=X-Api-Key
__apiKeyValue=pwd
__assetId=1
__assetDescription="Demo Asset"
__assetDataEndpoint=https://github.com/eclipse-dataspaceconnector

__asset="{
        \"asset\": {
            \"properties\": {
                \"asset:prop:id\": \"$__assetId\",
                \"asset:prop:description\": \"$__assetDescription\"
            }
        },
        \"dataAddress\": {
            \"properties\": {
                \"type\": \"HttpProxy\",
                \"endpoint\": \"$__assetDataEndpoint\"
            }
        }
    }"

# Call Data Management API
curl -X POST "$__connectorUrl/$__dataMgmtPath/assets" --header "$__apiKey: $__apiKeyValue" --header "Content-Type: application/json" --data "$__asset"
```

#### Bash Parameters

| Name                 | Description                                                                               |
| -------------------- | ----------------------------------------------------------------------------------------- |
| $__connectorUrl      | URL of the Connector with the Data Management API port configured in `web.http.data.port` |
| $__dataMgmtPath      | Path of the Data Management API as configured in `web.http.data.path`                     |
| $__apiKey            | Should always be _X-Api-Key_ for the Data Management API                                  |
| $__apiKeyValue       | The API Key Value as configured in `edc.api.auth.key`                                     |
| $__assetId           | Unique identifier of the asset                                                            |
| $__assetDescription  | Description of the asset                                                                  |
| $__assetDataEndpoint | Endpoint that might be used when data is transferred. Irrelevant in this context / sample |

#### Control Call

Get Asset

```bash
curl -X GET "$__connectorUrl/$__dataMgmtPath/assets/$__assetId" --header "$__apiKey: $__apiKeyValue" --header "Content-Type: application/json" | jq
```

### 2. Create Policy

**Please be aware that the following policy make the data offer public for everyone and should be used with caution outside of this showcase!**

Create a policy that can be used by the __ContractDefinition__. As the same policy may be used as contract- and access policy of the ContractDefinition, creating only one policy for both cases is totally fine for this demo.

#### Bash Script

```bash
# Variables
__connectorUrl=http://localhost:8181
__dataMgmtPath=data-mgmt
__apiKey=X-Api-Key
__apiKeyValue=pwd
__policyId=1

__publicPolicy="
{
    \"uid\": \"$__policyId\",
    \"prohibitions\": [],
    \"obligations\": [],
    \"permissions\": [
        {
            \"edctype\": \"dataspaceconnector:permission\",
            \"action\": {
                \"type\": \"USE\" 
            },
        }
    ]
}"

# Call Data Mgmt API
curl -X POST "$__connectorUrl/$__dataMgmtPath/policies" --header "$__apiKey: $__apiKeyValue" --header "Content-Type: application/json" --data "$__publicPolicy"
```

#### Bash Parameters

| Name            | Description                                                                               |
| --------------- | ----------------------------------------------------------------------------------------- |
| $__connectorUrl | URL of the Connector with the Data Management API port configured in `web.http.data.port` |
| $__dataMgmtPath | Path of the Data Management API as configured in `web.http.data.path`                     |
| $__apiKey       | Should always be _X-Api-Key_ for the Data Management API                                  |
| $__apiKeyValue  | The API Key Value as configured in `edc.api.auth.key`                                     |
| $__policyId     | Unique identifier of the policy.                                                          |

#### Control Call

Get Policy

```bash
curl -X GET "$__connectorUrl/$__dataMgmtPath/policies/$__policyId" --header "$__apiKey: $__apiKeyValue" --header "Content-Type: application/json" | jq
```

### 3. Create Contract Definition

The following uses the previously created public policy make the data offer available for everyone.

#### Bash Script

```bash
# Variables
__connectorUrl=http://localhost:8181
__dataMgmtPath=data-mgmt
__apiKey=X-Api-Key
__apiKeyValue=pwd
__contractDefinitionId=1
__policyId=1
__assetId=1

__publicContractDefinition="
        {
            \"id\": \"$__contractDefinitionId\",
            \"accessPolicyId\": \"$__policyId\",
            \"contractPolicyId\": \"$__policyId\",
            \"criteria\": [
                {
                    \"left\": \"asset:prop:id\",
                    \"op\": \"=\",
                    \"right\": \"$__assetId\"
                }
            ]
        }"

# Call Data Mgmt API
curl -X POST "$__connectorUrl/$__dataMgmtPath/policies" --header "$__apiKey: $__apiKeyValue" --header "Content-Type: application/json" --data "$__publicContractDefinition"
```

#### Bash Parameters

| Name                    | Description                                                                               |
| ----------------------- | ----------------------------------------------------------------------------------------- |
| $__connectorUrl         | URL of the Connector with the Data Management API port configured in `web.http.data.port` |
| $__dataMgmtPath         | Path of the Data Management API as configured in `web.http.data.path`                     |
| $__apiKey               | Should always be _X-Api-Key_ for the Data Management API                                  |
| $__apiKeyValue          | The API Key Value as configured in `edc.api.auth.key`                                     |
| $__contractDefinitionId | Unique identifier of the contract definition.                                             |
| $__policyId             | Unique identifier of the policy. Must be the same ID as in step 2.                        |
| $__assetId              | Unique identifier of the asset. Must be the same ID as in step 1.                         |

#### Control Call

Get Contract Definition

```bash
curl -X GET "$__connectorUrl/$__dataMgmtPath/policies/$__policyId" --header "$__apiKey: $__apiKeyValue" --header "Content-Type: application/json" | jq
```

### 4. Get Contract Offer Catalog

The last call is not (yet) part of the Data Management API. Instead, the deprecated Control API is used. The extension
for the control API is part of the Catena-X images and usable.

----

**Please Note**

Don't confuse the deprecated Control API with another Control API of the connector, that is not deprecated.

----

#### Bash Script

```bash
# Variables
__connectorUrl=http://localhost:8181
__targetConnectorUrl=http://localhost:9292
__targetConnectorIdsPath=api/v1/ids
__defaultApiPath=api
__apiKey=X-Api-Key
__apiKeyValue=pwd
__contractDefinitionId=1
__policyId=1
__assetId=1

__publicContractDefinition="
        {
            \"id\": \"$__contractDefinitionId\",
            \"accessPolicyId\": \"$__policyId\",
            \"contractPolicyId\": \"$__policyId\",
            \"criteria\": [
                {
                    \"left\": \"asset:prop:id\",
                    \"op\": \"=\",
                    \"right\": \"$__assetId\"
                }
            ]
        }"

# Call Control API
curl -G -X GET $__connectorUrl/$__defaultApiPath/control/catalog --header "$__apiKey: $__apiKeyValue" --data-urlencode "provider=$__targetConnectorUrl/$__targetConnectorIdsPath/data" --header "Content-Type: application/json" -s | jq
```

#### Bash Parameters

| Name                      | Description                                                                                                                                       |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| $__connectorUrl           | URL of the Connector with the Control API port configured in `web.http.default.port`                                                              |
| $__defaultApiPath         | Path of the Control API as configured in `web.http.default.path`                                                                                  |
| $__apiKey                 | The API Key as configured in `edc.api.control.auth.apikey.key`                                                                                    |
| $__apiKeyValue            | The API Key Value as configured in `edc.api.control.auth.apikey.value`                                                                            |
| $__targetConnectorUrl     | URL of the Connector of the target connector with the IDS API port configured in `web.http.ids.port`(in the configuration of the other connector) |
| $__targetConnectorIdsPath | The IDS Path as configured in `web.http.ids.path` (in the configuration of the other connector)                                                   |

# Known Control Plane Issues

Please have look at all the open issues in the open source repository. The list below might not be maintained well and
only contains the most important issues.
EDC Github Repository https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues

### All Contract Offers and their data is public available

https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1137

### Contract Negotiation not working

https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1140

### Missing Input validation in the Data Management API

https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1111
