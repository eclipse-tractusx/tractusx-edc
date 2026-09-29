# Migration Guide `0.8.x -> 0.9.x`

This document outlines the necessary changes for migrating your tractusx-edc installation from version 0.8.0 to 0.9.0.
It also outlines some points that adopters and operators should pay close attention to when migrating from one version
to another.

This document is not a comprehensive feature list.

<!-- TOC -->
* [Migration Guide `0.8.x -> 0.9.x`](#migration-guide-08x---09x)
  * [1. Strict Policy Definition Validation](#1-strict-policy-definition-validation)
  * [2. Removal of Azure based distributions](#2-removal-of-azure-based-distributions)
  * [3. Updated and Deprecated BPNL Group operators](#3-updated-and-deprecated-bpnl-group-operators)
  * [4. Store Migrations](#4-store-migrations)
<!-- TOC -->

## 1. Strict Policy Definition Validation

A new feature was added with upstream EDC 0.11.0 that requires the connector to perform a deeper validation of a
policy definition during its creation/update. This feature prevents any new policy definition of having:

- an action, within a rule, that doesn't bound to any evaluation scope
- a constraint, within a rule, that its leftOperand is not bound to any evaluation scope or any evaluation function.

On simpler terms, it prevents the creation of any policy definition which will evaluate nothing.

This feature is enabled by default in the distributed tractusx-edc helm charts. In order to disable the feature or
if you don't use the distributed helm chart, and you want to enable this feature, it can be toggled via configuration
or environment variable using the following config:

`edc.policy.validation.enabled=true|false`

## 2. Removal of Azure based distributions

As they were previously marked for deprecation, the azure vault based distributions were effectively removed and WILL
NOT
be distributed any longer. Be aware this has nothing to do with the cloud related extensions we support, such as Azure
blob or AWS S3 provisioners and dataplanes. Those are still included as part of the base tractusx-edc distribution.ß

If you installation relied on any tractusx-edc `azure-vault` distribution (either docker image or helm chart), please
also be aware that you will no longer find a distribution for this or future releases.

## 3. Updated and Deprecated BPNL Group operators

Special attention is necessary towards policy definitions that contain BPNL group constrains using either the `eq` or
`neq` operators. These have been deprecated in favor of the `isAllOf`, `isAnyOf` and `isNoneOf` operators which are the
proper operators for sets.

Additionally, the behavior of the `isAllOf` operator has been fixed since it previously failed validation for BPNLs
that were assigned to 3 distinct groups, 2 of which were the same as listed in the policy constrain.
The `isAllOf` now checks if a certain BPNL is assigned to all the groups allowed in the policy definition.

The `IN` operator was also fixed, since it evaluated the same as the faulty `isAllOf` operator.
It now performs the same as the `isAnyOf` operator.

## 4. Store Migrations

For connector operators who maintain specific flyway migration files, please be aware of new migrations added
in these PRs:

- https://github.com/eclipse-tractusx/tractusx-edc/pull/1706
- https://github.com/eclipse-tractusx/tractusx-edc/pull/1713