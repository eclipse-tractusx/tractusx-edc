# Migration Guide `0.10.x -> 0.11.x`

This document outlines the necessary changes for migrating your tractusx-edc installation from versions 0.10.x to 0.11.0.
It also outlines some points that adopters and operators should pay close attention to when migrating from one version
to another.

This document is not a comprehensive feature list.

<!-- TOC -->
* [Migration Guide `0.10.x -> 0.11.x`](#migration-guide-010x---011x)
  * [1. DCP version 1.0](#1-dcp-version-10)
  * [2. Participant ID](#2-participant-id)
<!-- TOC -->

## 1. DCP version 1.0
Tractus-X EDC now uses DCP 1.0 by default — no switch is required. If DCP 0.8.1 was previously forced via `edc.dcp.v08.forced`, 
that setting should be removed; it is deprecated and no flag is provided for 1.0.

## 2. Participant ID
Previously, the BPN was used as the EDC's participant identifier. Starting with `0.11.0`, the primary participant
identifier is now the DID. As the BPN is still required for some processes and also will be used for communication
with older Tractus-X EDC versions, it now needs to be supplied via a new setting. Therefore, the configuration should
be updated as follows:

```properties
edc.participant.id=<did>
tractusx.edc.participant.bpn=<bpn>
```
