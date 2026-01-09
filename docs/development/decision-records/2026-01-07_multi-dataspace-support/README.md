# Multi Data Space Support

## Decision

The Tractus-X EDC will distinguish between generic and Catena-X specific implementations, to enable future multi data space support. Existing extensions will be refactored so that every extension can clearly be categorized into a `core` (data space agnostic implementation) and data space-specific implementation (e.g., `catena-x`). The decision aims to enable future runtime configurations, such as data space specific flavors (e.g., `catena-x`, `factory-x`, `construct-x`), building upon the new `core` as midstream. The migration will be silent, to have minimal impact on current Catena-X users, while newer extensions and runtime configurations will be developed externally and merged into `tractusx-edc` when reaching production status.

## Rationale

The Tractus-X EDC was one of the first production-ready Connector implementations based on the upstream Eclipse Data Space Components (EDC) project for the Catena-X data space. Since then, various new data spaces have emerged, which also require connector configurations but can't use the Eclipse Tractus-X EDC directly because it includes Catena-X-specific implementations (like the BPN, the CX-Policy, etc.). This has led to further projects, like the [Factory-X EDC](https://github.com/factory-x-contributions/factoryx-edc), which build upon the Eclipse Tractus-X EDC as midstream but in a complex way using several exclusions. While other data spaces like Construct-X and Semiconductor-X also need a Connector without Catena-X specific aspects, this decision record aims to consolidate and bundle all development power into this Tractus-X EDC project to create an agnostic midstream `core` that can be used in other contexts than Catena-X. The rationale is supported by the [new Eclipse Tractus-X strategies to open and support multiple data spaces](https://github.com/eclipse-tractusx/eclipse-tractusx.github.io/pull/1370).

## Approach

This approach and work procedure is detailed in the following.

### Runtimes Concept & Structure

**Create one `core` data space agnostic runtime configuration and enable specific runtime flavors like `catena-x`, `factory-x`, and `construct-x`.**

Currently, there are two runtime configurations available: the [`edc-controlplane-base`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-controlplane/edc-controlplane-base) and the [`edc-dataplane-base`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-dataplane/edc-dataplane-base). Since these configurations are Catena-X specific, a new structure need to distinguish midstream `core` runtimes from data space specific runtimes.

One possible example of how a future structure might look:
```
tractusx-edc/
├── edc-controlplane
│   ├── edc-controlplane-core
│   ├── edc-controlplane-catena-x
        ├── edc-controlplane-base
        ├── edc-controlplane-postgresql-hashicorp-vault
        └── edc-runtime-memory
│   ├── edc-controlplane-factory-x
│   ├── edc-controlplane-construct-x+
│   └── ...
└── edc-dataplane
    ├── edc-dataplane-core
    ├── edc-dataplane-catena-x
        ├── edc-dataplane-base
        └── edc-dataplane-hashicorp-vault
    ├── edc-dataplane-factory-x
    ├── edc-dataplane-construct-x
    └── ...
```

### Extensions

**Keep all extensions in the `edc-extensions` folder in this repo. Refactor some existing extensions that are needed across multiple data spaces (`core`) to be Catena-X-independent.** 

Currently, the `edc-extensions` folder contains different extensions. They can be categorized into three categories:

1. **Generic extensions:** Data space agnostic, like the `agreement` extension
2. **Data space specific extensions:** Only made for one data space, like the `cx-policy` extension
3. **Generic, but specific:** Have a generic need and concept, but the implementation is currently data space specific, like the `dcp` extension (should be (1.) generic, but the implementation is currently specialized to (2.), the Catena-X data space)

All extensions from category (3.) will be refactored and split to distinguish between (1.) generic, and (2.) data space specific in the future.
As an example, the current `dcp` extension could be split into a `dcp-core` extension, belonging to (1.) generic and a `cx-dcp` extension, belonging to (2.), including the Catena-X specific implementation.

The following extensions belong to category (3.) and need to be refactored:

- [`connector-discovery`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/connector-discovery) (uses BDRS, a catena-x specific identity model, but is currently under refactoring)
- [`dataspace-protocol`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/dataspace-protocol) (supports multiple versions, incl. BPN, as catena-x specific)
- [`dcp`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/dcp) (interwoven with catena-x)
- [`migrations`](https://github.com/eclipse-tractusx/tractusx-edc/tree/main/edc-extensions/migrations) (interwoven with catena-x)

Further, the Factory-X project [published their extensions in another repository](https://github.com/factory-x-contributions/factoryx-edc/tree/main/edc-extensions). These extensions could be integrated and merged into this Eclipse Tractus-X EDC repository when reaching production status, providing a single place for all extensions.

### Development inside/outside `tractusx-edc`

**Necessary refactoring is made directly inside the `tractusx-edc` repository, while new developments are conducted externally.**

To enable multi data space support, the refactoring of the previously described extensions have to be made directly inside this `tractusx-edc`. The development of new runtimes and extensions is first developed externally, and potentially merged into `tractusx-edc` when reaching production status. _External_ could refer to closed-source or open-source development work in other (GitHub) organizations as well as development directly inside Eclipse Tractus-X, but in a new repository.


### Releases, Backward Compatibility

**Create a silent transition with minimal impact for existing Catena-X users.**

Since this repository is only used by the Catena-X data space participants, the overall goal is to implement the suggested changes with minimal impact, so that existing users will not notice them or only notice them slightly. Possible breaking changes should be reduced to a minimum. Such breaking changes may occur due to new folder structures and (re)namings of runtimes and extensions. Nevertheless, the naming of existing Docker images, Helm charts, and Maven artifacts should stay as long as possible and reasonable.

Regarding the releases, future Tractus-X releases should include the Catena-X flavor connector only in the first run. Potentially new connector flavors of the other data spaces should not be included in the release in the first step. This helps create a fast transition, without large alignment between the Tractus-X planning and release cycles and the cycles and procedures of the other data spaces. Future adjustments to also publish the other flavors in the Eclipse Tractus-X release are possible, but not part of this decision record.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2026 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V. (represented by Fraunhofer ISST)
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
