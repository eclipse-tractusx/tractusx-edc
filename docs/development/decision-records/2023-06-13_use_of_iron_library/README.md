# Usage of `iron-verifiable-credentials`

## Decision

Tractus-X EDC will use the [iron-verifiable-credentials](https://github.com/filip26/iron-verifiable-credentials) library
for all processing of VerifiableCredentials and VerifiablePresentations.

## Rationale

The Eclipse Dataspaces Components project uses iron's sister
library, [titanium-json-ld](https://github.com/filip26/titanium-json-ld/) for processing JSON-LD, which achieves close
to 100% of conformance with the JSON-LD specification.

It thus stands to reason that we use `iron-verifiable-credentials`, because it supports issuing/verifying VCs/VPs, has
support for JSON-LD (internally it also uses `titanium-json-ld`) and otherwise has a very light dependency footprint.
which means high runtime type compatibility can be expected and minimal mapping/compatibility layers are needed. Crypto
suites are pluggable, so in addition to `iron-ed25519-cryptosuite-2020`, which is also provided, we will implement
support for `JsonWebKey2020` which was mandated by the Catena-X consortium.

## Approach

- add support for `JsonWebKey2020` to Tractus-X EDC using `iron-verifiable-credentials`.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
