# Business Partner Validation Extension

This extension is used to introduce the capability to a connector to evaluate two types of policies:

- A Business Partner Group policy: evaluates, whether a certain BPN belongs to a certain group. For example, a
  participating company categorizes other dataspace participants in three
  groups: `"customer"`, `"gold_customer"`, `"platin_customer"`. Then, that company may want to show certain assets only
  to a specific group. The Business Partner Group Policy enables that semantic.
- [not recommended] a Business Partner Number Policy: evaluates, whether the BPN in question is contained in a list of "
  white-listed" BPNs. That whitelist is hard-coded directly on the policy. This policy is **not recommended anymore**
  due to
  concerns of scalability and maintainability. Each time such a policy is evaluated, the runtime will log a warning.

Technically, both these policies and their evaluation functions can be used in several circumstances, which in EDC are
called *scopes*. More information on how to bind policy functions to scopes can be found in
the [official documentation](https://eclipse-edc.github.io/documentation/for-adopters/control-plane/policy-engine/).

Both previously mentioned evaluation functions are bound to the following scopes:

- `catalog`: determines, what policies (specifically: constraints) are to be evaluated when requesting the catalog (
  i.e. "access policy")
- `contract.negotiation`: determines, which policies/constraints are to be evaluated during the negotiation phase (
  i.e. "contract policy")
- `transfer.process`: determines, which policies/constraints are to be evaluated when performing a data transfer, e.g.
  contract expiry

## Business Partner Group Policy

This policy states, that a certain BPN must, may or must not be member of a certain group. Groups may be represented as
scalar, or as comma-separated lists. For semantic expression, the following ODRL operators are
supported:
- `eq`
- `neq`
- `isPartOf`
- `isAllOf`
- `isAnyOf`
- `isNoneOf` 

The `eq` and `neq` operators are **deprecated** in favor of `isAllOf`, `isAnyOf` and `isNoneOf` operators.

The following example demonstrates a full JSON-LD structure in expanded form, containing such a constraint.

### Example

```json
{
  "@type": "https://w3id.org/edc/v0.0.1/ns/PolicyDefinitionDto",
  "https://w3id.org/edc/v0.0.1/ns/policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "permission": {
      "action": "USE",
      "constraint": {
        "@type": "http://www.w3.org/ns/odrl/2/LogicalConstraint",
        "or": [
          {
            "@type": "http://www.w3.org/ns/odrl/2/Constraint",
            "leftOperand": "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup",
            "operator": "http://www.w3.org/ns/odrl/2/isAllOf",
            "rightOperand": "greek,philosopher"
          }
        ]
      }
    }
  },
  "@id": "some-policy-id"
}
```

The first important take-away is the `constraint` object, which contains a single expression that mandates, that in
order to fulfill the policy, a business partner must be `greek` and they must be a `philosopher`. Whether a
particular BPN has either of these groups assigned is determined by the `ParticipantAgent`, and by a subsequent lookup
in an internal database. See [the next section](#manipulating-groups) for details.

The second important aspect is the `leftOperand`, which must
be `"https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup"`. Together with the scope, the `leftOperand` determines,
which constraint functions is called to evaluate the policy. Here, it is the `BusinessPartnerGroupFunction`.

### Manipulating groups

The `bpn-evaluation` module provides a simple CRUD REST API to manipulate associations between BPN and group. Each BPN is stored
in an internal database together with the groups that it was assigned. The OpenAPI specification can be
found [here](https://eclipse-tractusx.github.io/tractusx-edc/openapi/control-plane-api/) and [here](https://eclipse-tractusx.github.io/tractusx-edc/openapi/data-plane-api/) .

## Business Partner Number Policy [not recommended]

This policy mandates, that a particular Business Partner Number must be contained in a white-list that is hard-coded on
the policy. Here, only the ODRL `eq"` operator is supported, and the `rightOperand` must be the white-listed BPN.

### Example

```json
{
  "@type": "https://w3id.org/edc/v0.0.1/ns/PolicyDefinitionDto",
  "https://w3id.org/edc/v0.0.1/ns/policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "permission": [
      {
        "action": "USE",
        "constraint": {
          "@type": "http://www.w3.org/ns/odrl/2/LogicalConstraint",
          "or": [
            {
              "@type": "http://www.w3.org/ns/odrl/2/Constraint",
              "leftOperand": "BusinessPartnerNumber",
              "operator": "eq",
              "rightOperand": "BPN00001234"
            }
          ]
        }
      }
    ]
  },
  "@id": "some-policy-id"
}
```

Again, the `leftOperand` must be `"BusinessPartnerNumber`, and it determines, which constraint function is evaluated
(here: `BusinessPartnerPermissionFunction`). The evaluation of the example policy only succeeds, when
the `ParticipantAgent`'s BPN is `"BPN00001234"`.

In case multiple BPNs are to be white-listed, the policy would contain multiple `or` constraints:

```json
{
  "@type": "https://w3id.org/edc/v0.0.1/ns/PolicyDefinitionDto",
  "https://w3id.org/edc/v0.0.1/ns/policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "permission": [
      {
        "action": "USE",
        "constraint": {
          "@type": "http://www.w3.org/ns/odrl/2/LogicalConstraint",
          "or": [
            {
              "@type": "http://www.w3.org/ns/odrl/2/Constraint",
              "leftOperand": "BusinessPartnerNumber",
              "operator": "eq",
              "rightOperand": "BPN00001234"
            },
            {
              "@type": "http://www.w3.org/ns/odrl/2/Constraint",
              "leftOperand": "BusinessPartnerNumber",
              "operator": "eq",
              "rightOperand": "BPN00005678"
            }
          ]
        }
      }
    ]
  },
  "@id": "some-policy-id"
}
```

The second policy expresses that the BPN of the participant in question must be either `"BPN00001234"`
*or* `"BPN00005678"`.

### Usage warning

The Business Partner Number Policy is _not_ recommended for production use because it is severely limited in terms of
scalability and maintainability. Everytime a new participant shall be added to a contract definition the data provider would need to create new contract definitions, effectively duplicating them. This is because policies can't be changed after a contract that contains the policy has been negotiated with the first partner. That would be a significant maintenance and migration effort.

> **_NOTE:_**  The Business Partner Number Policy was marked for deprecation but will be kept as it is convenient to use for simple use-cases and testing.  
