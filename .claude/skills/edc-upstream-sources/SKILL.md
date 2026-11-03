---
name: edc-upstream-sources
description: Read, search and diff upstream Eclipse EDC (org.eclipse.edc) source code and SQL schemas at the exact version Tractus-X EDC depends on, or any other version. Use whenever an answer or change depends on upstream behaviour, such as an SPI signature, a default implementation, a state machine, a @Setting key, a JSON-LD transformer, a DSP/DCP implementation or a store schema, instead of answering from memory or from a local clone on the wrong version.
---

<!--
 Copyright (c) 2026 Catena-X Automotive Network e.V.
 Copyright (c) 2026 Contributors to the Eclipse Foundation

 See the NOTICE file(s) distributed with this work for additional
 information regarding copyright ownership.

 This program and the accompanying materials are made available under the
 terms of the Apache License, Version 2.0 which is available at
 https://www.apache.org/licenses/LICENSE-2.0.

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations
 under the License.

 AI Disclosure: This file was entirely AI-generated.
 The AI-generated portions are made available under CC0-1.0 and not subject
 to the project's license. The human contributor has reviewed and verified
 that the code is correct.

 SPDX-License-Identifier: Apache-2.0 AND CC0-1.0
 Assisted-By: Claude Code (claude-opus-5-5)
-->

# EDC upstream sources

Tractus-X EDC is a distribution of upstream EDC. Most behaviour lives upstream, and upstream changes quickly. This
skill gives exact, version-pinned access to upstream code without cloning anything.

The script `scripts/edc-src.sh`:
- reads the pinned version from `gradle/libs.versions.toml` (`edc = "…"`)
- takes sources jars from the local Gradle cache and downloads missing ones from Maven Central
- extracts them once to `~/.cache/edc-sources/<version>/<artifact>/` (override with `EDC_SRC_CACHE`), never into the repository

The first run per version takes up to a minute; later runs are instant. Set `EDC_SRC_OFFLINE=1` to forbid downloads.

## Commands

Run from anywhere inside the repository:

```bash
S=.claude/skills/edc-upstream-sources/scripts/edc-src.sh
$S version                                   # pinned upstream version, e.g. 0.17.0
$S find ContractNegotiationManager           # artifact + path of every matching source file
$S read DcpIdentityService                   # print the best match with line numbers (other matches listed)
$S read 'runtime/metamodel/annotation/Setting'   # disambiguate with a path fragment
$S grep 'implements PolicyEngine'            # extended-regex search across all upstream sources
$S settings 'edc.participant'                # upstream @Setting keys (filter is a regex)
$S schema                                    # list upstream SQL store schemas
$S schema contract-negotiation               # print one schema
$S diff EndpointDataReferenceEntry 0.16.0 0.17.0   # unified diff of a class between versions
$S path 0.16.0                               # extract another version and print its directory
```

To compare schemas between versions:
`diff <($S schema transfer-process 0.16.0) <($S schema transfer-process 0.17.0)`.

## How to use it well

1. **Pin first.** Run `$S version` and quote it in your answer ("in EDC 0.17.0, `X` does …").
2. **Cite precisely** as `org.eclipse.edc:<artifact>:<version>` plus the path and line from `read` output, e.g.
   `org.eclipse.edc:decentralized-claims-service:0.17.0 → org/eclipse/edc/iam/decentralizedclaims/service/DcpIdentityService.java:62`.
3. **Follow the call chain.** Start from the Tractus-X class (`git grep`), find the upstream type it imports, `read`
   it, then `grep` for implementations (`implements <Type>`) and registrations (`@Provider`, `register(`).
4. **Know the local overrides.** Some `org.eclipse.edc.*` packages are defined in this repo, mainly the DSP 0.8
   backports under `edc-extensions/dsp/*-08` and `spi/dsp-spi-08`. If `find` returns nothing, run
   `git ls-files '*/<Class>.java'` before concluding a type does not exist.
5. **Cover other groups.** Artifacts outside `org.eclipse.edc` (for example `org.eclipse.edc.aws`) are not
   indexed. Read them from GitHub at the matching tag instead:
   `https://github.com/eclipse-edc/<Repo>/tree/v<version>`.
6. **Treat local clones with care.** A sibling clone such as `~/eclipse-edc/Connector` may be on another version.
   Prefer this skill, or use `git -C <clone> show v<version>:<path>`.

## Typical questions this answers

- "Which policy scope does upstream evaluate on a catalog request?" `grep` for `CatalogPolicyContext` and `read`
  the evaluating service.
- "What does setting `edc.x.y` default to?" `settings edc.x.y` lists the key; `grep '"edc\.x\.y"'` finds the
  declaration with its `defaultValue`.
- "Did upstream change the transfer-process table?" `schema transfer-process` in both versions, then `diff`.
- "Why does my extension's override not apply?" `read` the default `@Provider` upstream and check whether it is
  `isDefault = true`.
