# Concept for Transfer of Encrypted Data via EDC

## Goal

Our users require a way to share encrypted data via the EDC.
A solution needs to satisfy these requirements:

- Encryption can be required by provider, recipient or both.
- This requirement can be defined on a per-asset basis.
- Negotiation of the encryption requirement must happen within the EDC contract framework.
- Encryption of data and transfer of public keys must happen within the EDC data transfer framework.
- Private keys must not be transferred.

## Proposal

### Contract Negotiation

The contract negotiation process must allow for a formalized way to require encryption of assets related to a contract.
To this end, a policy can be defined using a [duty-type rule as defined by ODRL](https://www.w3.org/TR/odrl-model/#duty).
Such a rule and a description of its parameters can be found [here](./ODRL_duty_convention.json).
Please note that this JSON file only shows the duty object, not the entire policy context it would exist in.

### Transfer Process

The transfer process needs to allow for the transfer of encryption keys and for the data encryption itself.
This could be implemented as either one extension or two, one for each of these two tasks.
The key transfer needs to follow [this convention](./JWK_convention.json)
based on the [JWK standard](https://datatracker.ietf.org/doc/html/rfc7517#section-4).
The data encryption must be applied on the provider's side on a per-asset and per-recipient basis.
For this purpose the extension needs to integrate with existing Transfer Process extensions.

## Further Considerations

### Consequences

The ODRL model allows for consequences to be applied in the case a duty is not exercised.
What exactly such a consequence might be, depends a lot on the legal framework in which the data transfer takes place.
While generic policy parameters can always be added to EDC policies in form of plain Strings,
this is not ideal in the long run.

### Verification of continued encryption

If encryption of assets on the side of the recipient is required,
and if consequences can apply in the case of non-compliance,
then the encryption must be verified throughout the duration of the contract.
How this can be done on a technical level is an open question (to me).
