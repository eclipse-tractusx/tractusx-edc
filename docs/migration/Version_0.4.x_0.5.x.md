# Migration from 0.4.x to 0.5.x

## Replacing DAPS with SSI

DAPS was deprecated as identity provider, and was replaced with an Self-Sovereign-Identity solution based on a
centralized Managed Identity Wallet (MIW) using VerifiableCredentials. Initially, there will be one SummaryCredential,
which conflates all relevant information. This is intended as interim solution and will later be replaced with a more
appropriate structure.

### Relevant terminology

Please make sure to be at least somewhat familiar with the following terms before you read on:

- VerifiableCredential
- VerifiablePresentation
- JWT - JSON Web Token
- DSP - the DataSpace Protocol

### Preconditions

All of these preconditions must be met before Tractus-X EDC `v0.5.x` is usable in a production use case. Please read
them carefully and make sure you understand the implications.

- every connector instance must have a
  signed [SummaryCredential](https://github.com/eclipse-tractusx/ssi-docu/tree/main/docs/credentials/summary) sitting in
  the MIW. This is typically done by the Portal during participant onboarding.
- the connector must have an account with KeyCloak and be able to obtain access tokens.
- the connector must be able to reach both MIW and KeyCloak via HTTP

### Authentication flow - quick intro

The basic workflow for a connector runtime to authenticate an incoming request is described in this section. Please note
that this procedure is limited to connector-to-connector communication via the Dataspace Protocol (DSP), it does not
relate to other APIs such as the Management API.

When a request is made by the Consumer, it obtains an access token from KeyCloak, which it uses to authenticate to MIW.
It then requests its SummaryCredential from MIW, which is returned in the form of a signed JWT that contains a
VerifiablePresentation (VP). That JWT is attached to the outgoing request as authorization header.
The Provider then decodes the JWT, validates its claims, and then uploads the VP to MIW for verification. Upon
successful verification, the Provider proceeds to process the request.

Please also check
out [this flow diagram](https://github.com/eclipse-tractusx/ssi-docu/blob/main/docs/architecture/cx-3-2/flow.svg) and
the associated [documentation](https://github.com/eclipse-tractusx/ssi-docu/tree/main/docs/architecture/cx-3-2).

### Noteworthy things and Caveats

- the MIWs REST API is secured with a token that can be obtained from a KeyCloak instance. This KeyCloak instance must
  be configured appropriately ahead of time.
- connectors have to be able to obtain a token from KeyCloak, so it must have an account with that KeyCloak instance
- we do **not** ship either MIW or KeyCloak nor do we provide support for either of them. Please contact the respective
  Tractus-X projects for instructions how to set them up.
- our official Helm charts now use SSI instead of DAPS. However, the charts do **not** include a dependency onto MIW of
  KeyCloak, nor do they contain configuration for them. They do, however, contain a configuration section (titled `ssi`)
  that configures EDC.
- our Helm charts can be installed, and the connector application will boot up, but unless MIW and KeyCloak are
  configured properly and both can be reached over network by the connector, every DSP request to another connector will
  fail. However, the ManagementAPI can still be used to create Assets, Policies and ContractDefinitions.
- the centralized MIW is an interim solution, and is bound to be replaced with a decentralized/distributed architecture
  in upcoming releases.

### Fallback chart

There is one Helm chart named `tractusx-connector-legacy` that is a carbon-copy of the old connector chart using DAPS.
It is not recommended for production use anymore and is solely intended as a fallback or as a way to gradually move to
SSI. We do not test it, nor do we provide support for it after the release of Tractus-X EDC `0.5.0`.

## The Observability API changes

The following settings are removed because the `observability-api-customization` extension will be no longer used.
The `Observability API` extension will be used instead

- `observability.port`
- `observability.path`
- `observability.insecure`

The status (`/health`, `/startup`, `/liveness`, `/readiness`) of the EDC can be checked by using the default endpoint.
