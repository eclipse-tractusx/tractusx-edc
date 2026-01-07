# Multi Data Space Support

## Decision

The Tractus-X EDC will support multiple data spaces beyond Catena-X. It will offer different extensions and runtimes, organized in a `core` package containing all data space-independent logic, and specific flavors (e.g., `catena-x`, `factory-x`, `construct-x`) that include data space-specific configurations. The current runtimes and extensions will be refactored and restructured so that each extension can be mapped to `core` or a data space-specific flavor. Thus, each data space has its own configurations and set of extensions, while all depend on the `core`. The migration will be silent, to prevent breaking changes for current Catena-X users.

## Rationale

The Tractus-X EDC was one of the first production-ready Connector implementations based on the upstream Eclipse data space Components (EDC) project for the Catena-X data space. Since then, various new data spaces have emerged, which also require connector configurations but can't use the Eclipse Tractus-X EDC directly because it includes Catena-X-specific implementations (like the BPN, the CX-Policy, etc.). This has led to further projects, like the [Factory-X EDC](https://github.com/factory-x-contributions/factoryx-edc), which build upon the Eclipse Tractus-X EDC. While other data spaces like Construct-X and Semiconductor-X also need a Connector without Catena-X specifica, this decision record aims to consolidate and bundle all development power into this Tractus-X EDC project to be able to use it in other contexts than Catena-X. The rationale is supported by the [new Eclipse Tractus-X strategies to open and support multiple data spaces](https://github.com/eclipse-tractusx/eclipse-tractusx.github.io/pull/1370).

## Approach

## Runtimes Concept & Structure
**Create one `core` configuration for all data spaces and specific configuration flavors like `catena-x`, `factory-x`, and `construct-x`.**

Currently, there are two runtime configurations available: the [`edc-controlplane-base`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-controlplane/edc-controlplane-base) and the [`edc-dataplane-base`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-dataplane/edc-dataplane-base). These configurations will be renamed with a `-catena-x` suffix, and a new `-core` configuration will be created, only containing all necessary extensions used by all data spaces. The other data spaces create their own configurations and runtimes based on the `-core` in their own folders. Afterward, the `-catena-x` configuration will be changed so that it also depends fully on the core.

This would result in the following new structure:
```
tractusx-edc/
├── edc-controlplane
│   ├── edc-controlplane-core
│   ├── edc-controlplane-catena-x (renamed from edc-controlplane-base)
│   ├── edc-controlplane-factory-x
│   ├── edc-controlplane-construct-x
│   └── ...
└── edc-dataplane
    ├── edc-dataplane-core
    ├── edc-dataplane-catena-x (renamed from edc-controlplane-base)
    ├── edc-dataplane-factory-x
    ├── edc-dataplane-construct-x
    └── ...
```

## Extensions
**Keep all extensions in the `edc-extensions` folder in this repo. Refactor some existing extensions that are needed across multiple data spaces (`core`) to be Catena-X-independent.** 

Currently, the `edc-extensions` folder contains different extensions. They can be categorized into three categories:

1. **Generic extensions:** Data space agnostic, like the `agreement` extension
2. **Data space specific extensions:** Only made for one data space, like the `cx-policy` extension
3. **Something in between:** Like the `dcp` extension (this should be (1.) generic, but is currently (2.) specific)

All extensions from category (3.) will be refactored so that every extension is either (1.) generic or (2.) data space specific.

The following extensions belong to category (3.) and need to be refactored:

- [`connector-discovery`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/connector-discovery) (uses BDRS, a catena-x specific identity model)
- [`dataspace-protocol`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/dataspace-protocol) (supports multiple versions, incl. BPN, as catena-x specific)
- [`dcp`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/dcp) (interwoven with catena-x)
- [`migrations`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/migrations) (interwoven with catena-x)

Those listed extensions will be split into parts, such as splitting `dcp` into `dcp-core`, which can be used by all data spaces, and `dcp-catena-x`, which includes Catena-X-specific aspects not part of `dcp-core`.

Further, the Factory-X project [published their extensions in another repository](https://github.com/factory-x-contributions/factoryx-edc/tree/main/edc-extensions). These extensions will be integrated and merged into this Eclipse Tractus-X EDC repository, providing a single place for all extensions.

## Releases, Backward Compatibility
**Create a silent transition with minimal impact for existing Catena-X users.**

Since this repository is only used by the Catena-X data space participants, the overall goal is to implement these new configuration flavors and extensions without breaking changes, so that existing users will not notice them or only notice them slightly.

Regarding the releases, future Tractus-X releases should include the Catena-X flavor connector only in the first run. The other connector flavors of the other data spaces should not be included in the release in the first step. This helps create a fast transition, without large alignment between the Tractus-X planning and release cycles and the cycles and procedures of the other data spaces. Future adjustments to also publish the other flavors in the Eclipse Tractus-X release are possible.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
