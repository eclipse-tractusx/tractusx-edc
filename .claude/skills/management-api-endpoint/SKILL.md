---
name: management-api-endpoint
description: Add, change, version or deprecate a Tractus-X EDC HTTP endpoint (Management API extensions such as /v3/edrs, /v3/business-partner-groups, /v3/connectordiscovery, /v3/dataflows, or data plane public, proxy and token APIs). Covers the API interface with OpenAPI annotations, controller, JSON-LD transformers and validators, registration in an API context, API tests, the OpenAPI spec and the walkthrough docs. Use for "add an endpoint", "move v4alpha to v3", "why does my request return 400", or to list the endpoints Tractus-X adds.
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

# Management API endpoint

Upstream EDC provides the core Management API (assets, policies, catalog, negotiations, transfers, …). Tractus-X adds its
own endpoints on top, and those are the ones this skill covers. For upstream endpoints, read the upstream controllers
with `edc-upstream-sources` (for example `edc-src.sh find ContractNegotiationApiV3Controller`).

## 1. Inventory first

```bash
.claude/skills/management-api-endpoint/scripts/api-inventory.sh            # every Tractus-X endpoint
.claude/skills/management-api-endpoint/scripts/api-inventory.sh v4alpha    # filter by path regex
```

For each endpoint it shows the method, full path, controller, API context (`management`, `public`, `proxy`, …),
whether a controller test exists, whether the walkthrough in `docs/usage/` mentions it, and deprecation.

## 2. Anatomy (follow the existing modules)

Use `edc-extensions/dataplane/dataflow/dataflow-api` (small) or `edc-extensions/edr/edr-api-v2` (full JSON-LD) as the
template:

| Piece | Example | Notes |
|---|---|---|
| API interface with OpenAPI annotations | `v3/DataFlowV3Api` | `@OpenAPIDefinition(info = @Info(version = "v3"))`, `@Tag`, `@Operation`, `@ApiResponse` per status code. These feed the generated spec. |
| Versioned controller | `v3/DataFlowV3ApiController` | `@Path("/v3/<resource>")`, `@Consumes`/`@Produces(APPLICATION_JSON)`. Implements the interface and delegates to a version-independent base. |
| Shared base logic | `DataFlowApiController`, `BaseEdrCacheApiController` | Keeps v3 and deprecated versions behaviourally identical. |
| JSON-LD in and out | `JsonObjectTo…Transformer`, `JsonObjectFrom…Transformer` | Registered in the `TypeTransformerRegistry` for the management API context. Request bodies arrive **expanded**: read `EDC_NAMESPACE`/`TX_NAMESPACE` IRIs, not compact terms. |
| Validation | `…RequestValidator` | Registered in `JsonObjectValidatorRegistry` for the `@type`. Invalid input → `ValidationFailure` 400. |
| Errors | `exceptionMapper(Type.class, id)` and `ServiceResult` | `ObjectNotFoundException` → 404, `InvalidRequestException` → 400, `ObjectConflictException` → 409. Never return 500 for client errors. |
| Registration | `…ApiExtension` | `webService.registerResource(ApiContext.MANAGEMENT, controller)`. Data plane public APIs use their own context. |
| Auth | inherited from the context | The Management API context is protected by the configured auth (`web.http.management.auth.*`, API key or delegated). A new context needs its own auth decision. |

## 3. Versioning and deprecation

The Management API path carries the version (`/v3/...`). The repo has done this before: moving `v4alpha` endpoints to
`v3` in 0.13 (migration guide 0.12 → 0.13, section 2).
- **A new endpoint** goes under the current stable version (`/v3/...`), unless it is explicitly experimental
  (`/v4alpha/...`).
- **Changing a request or response shape** breaks clients: add the new shape next to the old one or under a new
  version.
- **Deprecating** means annotating the old interface and controller `@Deprecated(since = "<version>")`, keeping both
  registered for at least one release, adding a migration-guide row (old path → new path), and removing it later.
  `breaking-change-guard` flags a removed `@Path`.

## 4. Tests

- **Controller test** (`@ApiTest`) extending upstream `RestControllerTestBase` (`org.eclipse.edc.web.jersey.testfixtures`),
  with RestAssured calls against the in-process Jersey server and mocked services. See
  `BusinessPartnerGroupApiV3ControllerTest` / `BaseBusinessPartnerGroupApiControllerTest`.
- **Transformer and validator unit tests** next to the classes (`JsonObjectToConnectorDiscoveryRequestTest`, …).
- **End to end** through a real runtime in `edc-tests/e2e/management-tests` or the feature's suite (`e2e-test-author`
  skill).
- Run `./gradlew test -DincludeTags="ApiTest"` (CI job "API tests").

## 5. Spec and docs

- OpenAPI: `./gradlew resolve` generates YAML into `resources/openapi/yaml` (swagger config in the root
  `build.gradle.kts`, `resourcePackages = org.eclipse.tractusx.edc`). `publish-openapi-ui.yml` publishes it, so check
  the generated YAML for your operation.
- Walkthrough: add or extend a page in `docs/usage/management-api-walkthrough/` with a copy-pasteable request and
  response. Keep JSON-LD `@context` complete.
- Migration guide entry for new, changed or deprecated paths (`migration-guide-and-adr` skill).

## Debugging "400 Bad Request" or "ValidationFailure"

1. Read the response body. It lists the failing JSON-LD path and the validator message.
2. Check the `@context`. Without the right context, terms do not expand to the IRIs the validator expects
   (`https://w3id.org/edc/v0.0.1/ns/`, `https://w3id.org/tractusx/v0.0.1/ns/`, the Catena-X policy context).
3. Check the `@type` the validator is registered for. A wrong or non-expanded type **skips validation silently**:
   upstream `JsonObjectValidatorRegistryImpl.validate` defaults to success for unknown types (EDC 0.17.0). The failure
   then surfaces later, in the transformer or the service.
4. Compare with the request in the walkthrough or the E2E test for that endpoint.
