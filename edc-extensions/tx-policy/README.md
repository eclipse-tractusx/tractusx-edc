# TX Policy Extension

This extension introduces DID-based access policy constraints for Tractus-X EDC, under the
namespace `https://w3id.org/tractusx/policy/2.0.0/`.

It is a companion to the existing `cx-policy` extension, which provides BPN-based access
control (`BusinessPartnerNumber`). The two are complementary: `cx-policy` identifies
counterparties by their BPN, while `tx-policy` identifies them directly by their
Decentralized Identifier (DID).

## Why DID instead of BPN?

In DCP/IATP flows, the counterparty authenticates by presenting a Verifiable Presentation (VP).
The EDC extracts the DID from that VP and stores it as the `ParticipantAgent` identity.
Because the DID is already known at this point, no external lookup (e.g. BDRS) is needed to
enforce access control — the DID in the policy is compared directly against the identity of
the connecting party.

This makes `BusinessPartnerDID` policies simpler, faster and independent of the BDRS service.

## Technical approach

A single constraint function, `BusinessPartnerDidConstraintFunction`, is registered with the
EDC policy engine. When the engine evaluates a policy containing the `BusinessPartnerDID`
left-operand, it:

1. Reads the connecting party's identity from the `ParticipantAgent` (this is a DID, already
   resolved during the DCP handshake).
2. Compares it against the DID(s) in the right-operand of the constraint.
3. Grants or denies access based on the operator (`isAnyOf` / `isNoneOf`).

The function is registered for the `CatalogPolicyContext` and bound across all EDC scopes
(catalog, negotiation, transfer process) so that the engine evaluates it wherever access
policies are enforced.

## Supported left operand

| Left operand         | Full IRI                                                     |
|----------------------|--------------------------------------------------------------|
| `BusinessPartnerDID` | `https://w3id.org/tractusx/policy/2.0.0/BusinessPartnerDID` |

## Supported operators

| Operator   | Behaviour                                                  |
|------------|------------------------------------------------------------|
| `isAnyOf`  | Access granted if the agent's DID is in the list           |
| `isNoneOf` | Access granted if the agent's DID is **not** in the list   |

## How to use it

### Single DID

```json
{
  "@context": [
    "https://w3id.org/edc/v0.0.1/ns/",
    {
      "tx": "https://w3id.org/tractusx/policy/2.0.0/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "did-access-policy-001",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "tx:BusinessPartnerDID",
          "operator": "isAnyOf",
          "rightOperand": "did:web:portal-backend.tractus-x.com:api:administration:staticdata:did:BPNL00000003CRHK"
        }
      }
    ]
  }
}
```

### Multiple DIDs

Provide a JSON array as the `rightOperand` to allow more than one counterparty:

```json
{
  "@context": [
    "https://w3id.org/edc/v0.0.1/ns/",
    {
      "tx": "https://w3id.org/tractusx/policy/2.0.0/"
    }
  ],
  "@type": "PolicyDefinition",
  "@id": "did-access-policy-002",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "tx:BusinessPartnerDID",
          "operator": "isAnyOf",
          "rightOperand": [
            "did:web:portal-backend.tractus-x.com:api:administration:staticdata:did:BPNL00000003CRHK",
            "did:web:portal-backend.tractus-x.com:api:administration:staticdata:did:BPNL00000000001A"
          ]
        }
      }
    ]
  }
}
```

### Blocking a specific counterparty

Use `isNoneOf` to deny access to a specific DID while allowing everyone else:

```json
{
  "constraint": {
    "leftOperand": "tx:BusinessPartnerDID",
    "operator": "isNoneOf",
    "rightOperand": "did:web:portal-backend.tractus-x.com:api:administration:staticdata:did:BPNL00000003CRHK"
  }
}
```

## Difference to `BusinessPartnerNumber`

| Aspect              | `BusinessPartnerNumber` (cx-policy)        | `BusinessPartnerDID` (tx-policy)          |
|---------------------|--------------------------------------------|-------------------------------------------|
| Namespace           | `https://w3id.org/catenax/2025/9/policy/`  | `https://w3id.org/tractusx/policy/2.0.0/` |
| Right-operand       | BPN (`BPNL…`)                              | DID (`did:…`)                             |
| BDRS lookup         | Yes (when agent identity is a DID)         | No — identity is already a DID            |
| External dependency | BDRS service                               | None                                      |

