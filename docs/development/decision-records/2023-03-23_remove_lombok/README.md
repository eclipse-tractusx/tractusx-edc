# Remove Lombok from code base

## Decision

The Lombok library will be removed from the code base.

## Rationale

Lombok uses byte-code modification to achieve its goal. That is dangerous for a number of reasons.

First and foremost, to achieve its goal, it relies on internal APIs of the JVM, which are not intended for public
consumption, thus they can and will get removed, refactored or made otherwise unavailable. This has been discussed at
length in the [project's GitHub page](https://github.com/projectlombok/lombok/issues/2681).
This is especially problematic for an OSS project such as Eclipse Tractus-X.

Second, many of the features that are currently used by Tractus-X EDC are experimental (e.g. `@UtilityClass`) and are
known to break some Java standard features, such as static imports.

Third, the value that Lombok offers is questionable at best (e.g. various constructor
annotations, `@Builder`, `@Value`), because modern IDEs have ample features to generate boilerplate code. Further, it
makes the code arguably less readable and less debuggable, very non-resilient against
refactoring (`@ToString(of = <FIELDNAME>)`) and more dangerous (`@SneakyThrows`) at runtime.

Fourth and finally bytecode modification could conceivably cause problems in use cases where audited/certified code is
required. Since the code gets modified during compilation in a way not covered by any spec, technically the runtime code
could be significantly different from the source code. Although this problem is admittedly theoretical at the moment, we
should not build those obstructions into the code base.

## Approach

- Remove the lombok library from the version catalog
- replace all annotations with actual code
- [optional] add an entry to our coding principles to forbid byte-code modification (lombok, aspectJ,...)

## Further consideration

We can even expect a slightly faster build, because "delomboking" will become unnecessary.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
