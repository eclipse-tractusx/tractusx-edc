# Migration Version 0.1.0 to 0.1.1

This document contains a list of breaking changes that are introduced in version 0.1.1.

---

**Please Note**:
Due to a change in the DAPS authentication mechanism this version cannot exchange messages with older EDC versions!

---

## 0. Summary

1. Data Management API
   1. Policy Payload
2. Connector Configuration
   1. CX OAuth Extension


## 1. Data Management API

It might be necessary to update applications and scripts that use the Data Management API. This section covers the most
important changes in endpoints and payloads.

### 1.1 Policy Payload

The id field of the PolicyDefinition was renamed from `uid` to `id`.

<details>

<summary>Example</summary>

Old Call
```json
{
    "uid": "1",
    "policy": {
        "prohibitions": [],
        "obligations": [],
        "permissions": [
            {
                "edctype": "dataspaceconnector:permission",
                "action": {
                    "type": "USE"
                },
                "constraints": []
            }
        ]
    }
}
```

New call
```json
{
    "id": "1",
    "policy": {
        "prohibitions": [],
        "obligations": [],
        "permissions": [
            {
                "edctype": "dataspaceconnector:permission",
                "action": {
                    "type": "USE"
                },
                "constraints": []
            }
        ]
    }
}
```

</details>

## 2. Connector Configuration
### 2.1. CX OAuth Extension

All connectors are now shipped with a new OAuth extension. This extension has an additional mandatory setting called `edc.ids.endpoint.audience`, that must be set to the IDS path.

[Documentation](/edc-extensions/cx-oauth2/README.md)


<details>

<summary>Example</summary>

```
edc.ids.endpoint.audience=http://plato-edc-controlplane:8282/api/v1/ids/data
```

</details>
