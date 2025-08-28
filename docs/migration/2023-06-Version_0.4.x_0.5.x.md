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

### Connecting to MIW

For connector onboarding, please contact the Portal Team. After that, you should receive:

- the issuer BPN: this is the BPN of the MIW
- your client id: this is the KeyCloak Client ID
- your client secret: this the KeyCloak Client Secret. Please store this in a secure vault and remember the `alias`.
  *Do not leak or publish this!*

In order to establish a connection to MIW, and you are using EDC on code level, please be sure to
follow [this documentation](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/ssi/ssi-miw-credential-client).
If you are using the official Helm charts, please check
out [this documentation](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/charts/tractusx-connector/README.md).

If you are using the MIW and KeyCloak instances deployed to `INT`, the following values apply:

- MIW Url: <https://managed-identity-wallets-new.int.demo.catena-x.net>
- KeyCloak Token URL: <https://centralidp.int.demo.catena-x.net/auth/realms/CX-Central/protocol/openid-connect/token>
- Authority BPN: `BPNL00000003CRHK`

> Please be aware that the above values are *only* valid for the Catena-X INT environment and *will* change on other
> environments! For instructions on how to set up a local MIW + KeyCloak, please take a look
> at [this documentation](https://github.com/eclipse-tractusx/managed-identity-wallet/blob/features/java-did-web/README.md).

### Further documentation on SSI

Please find more information in
the [SSI Documentation Repository](https://github.com/eclipse-tractusx/ssi-docu/tree/main/docs/architecture/cx-3-2).

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
- At the time of releasing Tractus-X EDC `0.5.0`, a couple of critical issues regarding MIW are
  still [in progress](https://jira.catena-x.net/projects/CGD/issues/CGD-291), and the code base is still on a feature
  branch. For those reasons we recommend to use *non-production data only*!
- the centralized MIW is an interim solution, and is bound to be replaced with a decentralized/distributed architecture
  in upcoming Catena-X releases.

## The Observability API changes

The following settings are removed because the `observability-api-customization` extension will be no longer used.
The `Observability API` extension will be used instead

- `observability.port`
- `observability.path`
- `observability.insecure`

The status (`/health`, `/startup`, `/liveness`, `/readiness`) of the EDC can be checked by using the default endpoint.

## The Consumer Pull flow changes

Starting from `0.5.0-rc5` which incorporates `EDC` 0.1.3, the consumer pull has been simplified in upstream, and it
can cause some breaking changes on users usage. The change is reflected in
this [diagram](https://github.com/eclipse-edc/Connector/blob/v0.1.3/docs/developer/architecture/data-transfer/diagrams/transfer-data-plane-consumer-pull.png).

The main difference is that in the previous iteration of the pull flow there were two EDRs involved. One created by the
provider while serving
a transfer request, and one created by the consumer (wrapping the provider one). The consumer one then was dispatched to
the EDR receivers for requesting
the data via consumer dataplane.

In the current iteration the receivers now receive directly the provider EDR without the double "wrapping" and can be
used directly to fetch data
from the provider dataplane, without passing thought to the consumer dataplane.

The shape of the EDR has not been changed so, if in the backend systems the EDR#endpoint was used as url for fetching
data, it should not cause any
breaking changes.

If the backend system manually forward the EDR to the consumer dataplane or tries to decode it via consumer validate
token APIs,
this will not work with signature check errors, as the EDR is the one signed by the provider.

> Note the custom property `cid` in the EDR, it's not available anymore. The property it's still available inside the
> JWT `authCode`

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
