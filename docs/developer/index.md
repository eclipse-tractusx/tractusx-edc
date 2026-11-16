# For developers

You want to change the connector itself: fix a bug, add a feature, write an extension. This section takes you from a
fresh clone to a merged pull request.

<div class="grid cards" markdown>

-   :material-laptop:{ .lg .middle } __Setup__

    ---

    Toolchain, build commands, IDE code style, running a runtime locally.

    [:octicons-arrow-right-24: Set up](setup.md)

-   :material-family-tree:{ .lg .middle } __Code architecture__

    ---

    Repository layout, module layering, how runtimes are composed from extensions.

    [:octicons-arrow-right-24: Understand the code](code-architecture.md)

-   :material-puzzle:{ .lg .middle } __Writing extensions__

    ---

    `@Extension`, `@Inject`, `@Provider`, `@Setting`, and where a new module belongs.

    [:octicons-arrow-right-24: Extend](extensions.md)

-   :material-test-tube:{ .lg .middle } __Testing__

    ---

    Test tags, the end-to-end suites, and how to run exactly what CI runs.

    [:octicons-arrow-right-24: Test](testing.md)

-   :material-source-pull:{ .lg .middle } __Contributing__

    ---

    ECA, Conventional Commits, Checkstyle, license checks and decision records.

    [:octicons-arrow-right-24: Contribute](contributing.md)

-   :material-robot-happy:{ .lg .middle } __Connector Sensei__

    ---

    An AI agent with tested skills for upgrades, settings, migrations, endpoints and reviews.

    [:octicons-arrow-right-24: Meet the Sensei](../development/connector-sensei/README.md)

</div>

## The shortest path

```bash
git clone https://github.com/eclipse-tractusx/tractusx-edc.git
cd tractusx-edc
./gradlew build                     # compile, unit tests, Checkstyle
```

Then pick a [good first issue](https://github.com/eclipse-tractusx/tractusx-edc/labels/good%20first%20issue) and read
[Contributing](contributing.md).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
