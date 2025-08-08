# Migration Guide `0.10.x -> 0.11.x`

This document outlines the necessary changes for migrating your tractusx-edc installation from versions 0.10.x to 0.11.0.
It also outlines some points that adopters and operators should pay close attention to when migrating from one version
to another.

This document is not a comprehensive feature list.

<!-- TOC -->
* [Migration Guide `0.10.x -> 0.11.x`](#migration-guide-010x---011x)
  * [1. DCP version 1.0](#1-dcp-version-10)
<!-- TOC -->

## 1. DCP version 1.0
Tractus-X EDC now uses DCP 1.0 by default—no switch is required. If DCP 0.8.1 was previously forced via `edc.dcp.v08.forced`, 
that setting should be removed; it is deprecated and no flag is provided for 1.0. As a DID (not a BPN) is now used as 
the primary identifier, the EDC_PARTICIPANT_ID must be updated to the corresponding DID. Also message fields like `participantId`, 
`assignee`, and `assigner` carry DID values instead of BPNs.


