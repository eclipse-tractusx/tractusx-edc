# DID Service Registration

## Decision

The controlplane will be enabled to register itself as `DataService` with the participant's did document. There will be
configuration variables to enable the feature, set an id for the DSP endpoint and point to the DID service's write-APIs.
There will not be an additional endpoint on the Management API - this logic is purely internal.

## Rationale

Standard CX-0001 currently describes the predominant method for discovering DSP endpoints. It is a centralized service
that is assumed to be a singleton. DIDs provide all means to map from a business partner identifier to a DSP endpoint.
How that can be achieved is described in [DSP section 4](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1-err1/#discovery-of-service-endpoints)

Managing these `service` entries for DSP endpoints can become a chore: hosts may change, deployments may be 
deprovisioned. That's why there should be a solution that is extensible for each wallet implementation and smart enough 
to avoid creating duplicate `service` entries and manage itself.

## Approach

1. Introduce configuration options in application and helm chart.
2. Create a new SPI including an interface that represents the feature in an abstract manner.
3. Implement the interface as client for [SAP DIV's write endpoint to the did document](https://api.sap.com/api/DIV/path/CompanyIdentityV2HttpController_updateCompanyIdentity_v2.0.0).
4. The extension will perform self-cleanup in 

The SPI will look something like

```java

public interface DidServiceClient {

    void createService(String id, String urlOfWellKnown);
    
    void updateService(String id, String urlOfWellKnown);
    
    void deleteService(String id);
    
}

```