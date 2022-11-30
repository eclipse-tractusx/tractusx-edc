# Release Notes Version 0.1.3

30.11.2022

## 0. Summary

1. Container Images
    - New Image: HashiCorp Vault & In Memory Store
2. Extensions
    - Business Partner Extension
    - HashiCorp Vault Extension
    - OAuth2 Extension
3. Bug Fixes 
    - S3 Data Transfer

# 1. Container Images

## 1.1 New Image: HashiCorp Vault & In Memory Store

The EDC now releases a fourth image with a combination of HashiCorp Vault and In Memory Store extensions.

# 2. Extensions

## 2.1 Business Partner Extension

**Removed support for Constraint with multiple BPNs**
The possibility to use multiple Business Partner Numbers inside of a single constraint has been removed. It looks like
this was only possible due to a missing feature and may lead to unexpected side
effects (https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/2026)

Hence, this kind of policy is no longer supported!

```json
{
  "uid": "<PolicyId>",
  "prohibitions": [],
  "obligations": [],
  "permissions": [
    {
      "edctype": "dataspaceconnector:permission",
      "action": {
        "type": "USE"
      },
      "constraints": [
        {
          "edctype": "AtomicConstraint",
          "leftExpression": {
            "edctype": "dataspaceconnector:literalexpression",
            "value": "BusinessPartnerNumber"
          },
          "rightExpression": {
            "edctype": "dataspaceconnector:literalexpression",
            "value": [
              "<BPN1>",
              "<BPN2>"
            ]
          },
          "operator": "IN"
        }
      ]
    }
  ]
}
```

The BPN extension will now always decline BPN policies with 'IN' operators, when asked by the EDC to enforce it.

## 2.2 HashiCorp Vault Extension

It is now possible to arrange HashiCorp Vault secrets in sub-directories.

For example by storing the DAPS secrets in their own `/daps` directory:

```
EDC_OAUTH_PRIVATE_KEY_ALIAS: daps/my-plato-daps-key
EDC_OAUTH_PUBLIC_KEY_ALIAS: daps/my-plato-daps-crt
```

## 2.3 OAuth2 Extension

The EDC Oauth2 Extension has now the possibility to add the audience to the claim. As the official OAuth2 Extension was
added to the control plane again most of the functionality of the CX Oauth2 Extension was removed.

> **Breaking Change** The official OAuth2 Extension uses different settings then the EDC OAuth Extension. Please
> consolidate the [Migration Documentation](../migration/Version_0.1.2_0.1.3.md).

# 3. Bug Fixes

## 3.1 S3 Data Transfer

Version 0.1.2 had some issues with the S3 data transfer. This version fixes them.