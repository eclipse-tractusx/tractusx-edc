# Migration from 0.3.4 to 0.4.0

## Overview

The Eclipse Dataspace Connector protocol recently moved its protocol implementation from IDS to DSP as of
version `0.0.1-milestone-9`.
From the Tractus-X EDC perspective this causes breaking changes in the following areas:

- ***Protocol***
  - modules: all `*ids*` modules are deprecated and cannot be used anymore. Please migrate over
    to `org.eclipse.edc:dsp:0.0.1-milestone-9`.
  - Please note that this is not a complete documentation of the DSP Protocol. Please refer to:
    - [DSP Protocol - Official documentation](https://docs.internationaldataspaces.org/dataspace-protocol/overview/readme)


- ***Management API***
  - Because DSP uses JSON-LD, most of the Management API endpoints had to be adapted as well to reflect that.
    The old Management API is now deprecated and is **not** tested for compliance. Please upgrade using the `/v2/` path
    for every endpoint, e.g. `<PATH>/management/v2/assets`. 
  - Please also refer to:
    - [EDC OpenAPI spec](https://app.swaggerhub.com/apis/eclipse-edc-bot/management-api/0.0.1-SNAPSHOT#/)
    - [Management API usage guide](Version_0.3.4_0.4.0%20-%20Guide/1-management-api.md)


- ***Removal of the Business Tests***
  - The business tests were removed from the code base, because all the ever tested is already by other tests, specifically
  the JUnit-based tests, deployment tests, or other tests that are already done upstream in EDC. 
  - The Business tests were brittle, consumed a lot of resources and were quite cumbersome to run and debug locally.


- ***New implementation for the Control Plane Adapter***
  - Since the old Control-Plane-Adapter is incompatible with DSP, a new iteration was created.
  **Due to time constraints with this release documentation for this feature will to be published subsequently**

## Settings changes

| Property                 | Description                                                          | default                          |
|--------------------------|----------------------------------------------------------------------|----------------------------------|
| edc.participant.id       | Configures the participant id this runtime is operating on behalf of | anonymous                        |
| edc.dsp.callback.address | Configures dsp protocol callback address                             | http://localhost:8282/api/v1/dsp |

> Please note: The new DSP protocol is deployed under the protocol context, which new default values.

| Property                 | Description                              | default      |
|--------------------------|------------------------------------------|--------------|
| web.http.protocol.port   | Configures the protocol api context port | 8282         |
| web.http.protocol.path   | Configures the protocol api context path | /api/v1/dsp  |
