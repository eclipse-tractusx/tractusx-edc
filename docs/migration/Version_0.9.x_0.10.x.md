# Migration Guide `0.9.x -> 0.10.x`

This document outlines the necessary changes for migrating your tractusx-edc installation from versions 0.9.x to 0.10.0.
It also outlines some points that adopters and operators should pay close attention to when migrating from one version
to another.

This document is not a comprehensive feature list.

<!-- TOC -->
* [Migration Guide `0.9.x -> 0.10.x`](#migration-guide-09x---010x)
  * [1. DCP version 0.8 setting](#1-dcp-version-08-setting)
<!-- TOC -->

## 1. DCP version 0.8 setting

In the upstream EDC the development of the DCP v1.0 made the v0.8 sort of legacy, and it needs explicit configuration.
So this setting is required to permit correct interaction with the DIM:

`edc.dcp.v08.forced=true`
