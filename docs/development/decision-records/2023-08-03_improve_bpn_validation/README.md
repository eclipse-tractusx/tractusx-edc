# Improve BPN validation

## Decision

The BPN validation extension will be improved in the following aspects:

1. Instead of hard-coding them on policies, BPNs are stored in a database (in-mem + Postgres)
2. BPNs are grouped to enable stable policies
3. More `Operator`s are supported
4. Database entries can be manipulated using a REST API

## Rationale

Hard-coding BPNs on policies is quite inflexible and does not scale, because when a new business partner joins or leaves
the network, all participants would have to update all their policies, which is a significant migration effort. Instead,
a structure has to be defined where that situation can be handled in a less intrusive and involved way. This effectively
will remove the need to update/migrate policies.

## Approach

Every BPN is associated with one or more groups, for example `BPN0000001` -> `["gold_member"]`. It is important to note,
that these groups are _internal_ tags that every participant maintains on their own, they are not claims in
VerifiableCredentials (the BPN would be a claim, however). A new policy constraint is introduced, that looks like this:

```json
{
  "constraint": {
    "leftOperand": "https://w3id.org/catenax/2025/9/policy/BusinessPartnerGroup",
    "operator": "isAnyOf",
    "rightOperand": [
      "gold_customer",
      "platin_partner"
    ]
  }
}
```

NB: the `leftOperand` must be an IRI as mandated by ODRL, thus it must either be prefixed with the namespace (as shown
in the example), or using a vocabulary entry in the JSON-LD context, i.e. `tx:BusinessPartnerGroup`. Supported operators
will be: `eq, neq, in, isAllOf, isAnyOf, isNoneOf`.

Manipulating the BPN -> group associations can be done through a REST API.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
