# Legacy Data Plane Signaling Extension

Control plane extension that provides the `DataFlowController` for the legacy data plane signaling, the protocol the
Tractus-X data plane speaks. It is dataspace-agnostic.

Since EDC 0.18 the upstream control plane BOM ships the new Data Plane Signaling implementation
(`data-plane-signaling`) instead of the legacy one, and upstream deprecated its legacy module
`org.eclipse.edc:transfer-data-plane-signaling`. This extension is a backport of that module (EDC 0.18.2) and replaces
it. It is unchanged except for the resumption of suspended transfer processes:

- **Resume.** EDC 0.18 calls `DataFlowController#resume` on provider and consumer, and the upstream legacy controller
  implements it as `start` with an empty policy. On the provider, the data plane then receives no participant id, so
  the new EDR has no audience. On the consumer, which has no data flow for a PULL transfer, selecting a data plane fails
  and the transfer process is terminated. This extension restores the behaviour of EDC 0.17: the provider restarts the
  data flow with the policy of the contract agreement, and the consumer does not signal its data plane.
- **New EDR on the consumer after resume.** EDC 0.18 stores the data address the provider sends on resumption, but the
  `TransferProcessStarted` event that follows still carries the data address of the first start, so the EDR store
  keeps the revoked EDR. On resumption the consumer sets the stored data address as content data address of the
  transfer process, as EDC 0.17 did, so that the event carries the new EDR.

## Usage

The Tractus-X control plane (`edc-controlplane-base`) includes this extension and excludes the upstream module. Custom
runtimes must do the same, since both provide a `DataFlowController`. The extension fails at start-up when the upstream
module is on the classpath as well:

```kotlin
configurations.all {
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling")
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-core")
    exclude(group = "org.eclipse.edc", module = "data-plane-signaling-oauth2")
    exclude(group = "org.eclipse.edc", module = "transfer-data-plane-signaling")
}

dependencies {
    implementation("org.eclipse.tractusx.edc:legacy-data-plane-signaling:<version>")
}
```

The extension needs no other Tractus-X extension. A `DataFlowPropertiesProvider` is optional, as in upstream.

## Configuration

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `edc.dataplane.client.selector.strategy` | | `random` | Strategy for selecting a data plane instance when the data plane is not embedded in the control plane. Same setting as the upstream module. |

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: Apache-2.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
