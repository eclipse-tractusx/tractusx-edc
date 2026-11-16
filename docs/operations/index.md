# Operations

Running a connector day to day: knowing that it is healthy, finding out why something failed, and fixing it.

<div class="grid cards" markdown>

-   :material-chart-line:{ .lg .middle } __Observability__

    ---

    Health checks, logs, OpenTelemetry and business events.

    [:octicons-arrow-right-24: Observe](observability.md)

-   :material-speedometer:{ .lg .middle } __Tune for production__

    ---

    Log level, JVM memory, replicas, state machines, database pool, caches and timeouts.

    [:octicons-arrow-right-24: Tune](tuning.md)

-   :material-stethoscope:{ .lg .middle } __Troubleshooting__

    ---

    Common symptoms, their causes and the fix.

    [:octicons-arrow-right-24: Diagnose](troubleshooting.md)

-   :material-shield-check:{ .lg .middle } __Production checklist__

    ---

    The hardening steps before you go live.

    [:octicons-arrow-right-24: Harden](../deployment/production-checklist.md)

-   :material-update:{ .lg .middle } __Upgrades__

    ---

    Release model, support policy and the upgrade procedure.

    [:octicons-arrow-right-24: Upgrade](../deployment/upgrades.md)

</div>

## A healthy connector at a glance

```bash
kubectl get pods -l app.kubernetes.io/instance=<release>      # all Running and READY
helm test <release>                                           # chart test passes
kubectl logs deploy/<release>-controlplane | grep -i "ready"  # runtime finished booting
```

And from the outside: your partners can request your catalog, and your own catalog requests to them succeed.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: GitHub Copilot (claude-opus-5.5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
