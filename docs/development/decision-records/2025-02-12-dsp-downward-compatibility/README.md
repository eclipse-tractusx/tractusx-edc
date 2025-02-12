# Handling of downward compatibility for a DSP version

## Decision

DSP assets, like the catalog, which are forwarded directly to a caller through the management api, have to be kept downward compatible as long as the DSP version through which the asset is retrieved is supported. There is no compatibility requirement between two different DSP versions concerning such assets.

Relevant assets are:
- Catalog returned by...
- ...

## Rationale

DSP assets which are directly received by a caller of a management api endpoint is data that is transfered between two dataspace participants and creates a direct relationship between the calling application and the provider connector. As this is the case, the breaking change requirements of Catena-X has to be fulfilled. The requirement states that a consumer can continuously consume data from the provider without the need to be forced to update his system due to a change of the provider service stack. This requires that such assets must not change in a way, that existing properties vanish from the returned data.

As a consumer participant is capable to ensure the usage of a connector with a certain set of DSP versions supported, he is capable to use applications that are capable to support all potential data formats received by the different supported DSP versions. Therefore, the asset data models supported in two different DSP versions are not related and therefore no compatibility requirement is needed between those.

## Approach

- The compatibility tests are enriched by tests that ensure the detection of changes in the corresponding DSP assets.
- If changes are detected a manual check that the downward compatibility is kept is executed.
