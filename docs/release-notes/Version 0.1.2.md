# Release Notes Version 0.1.2
30.09.2022

> This version introduced mostly bugfixes and thread mitigation by updating libraries.

## 1. Eclipse Dataspace Connector

The Git submodule references commit `740c100ac162bc41b1968c232ad81f7d739aefa9` from the 23th of September 2022 (newer than **0.0.1-milestone-6**).

## 2. Product EDC

### 2.1 Alpine Image

Introduce alpine image as base for all Product EDC Images (replaced distroless image).

## 3. Fixed Issues

- Contract negotiation not working when initiated with policy id ([issue](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/1251))

- Negotiation of Policies with extensible properties now works as expected