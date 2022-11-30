# Migration Version 0.1.2 to 0.1.3

This document contains a list of breaking changes that are introduced in version 0.1.3.

## OAuth2 Extension

As the images now use the official OAuth2 Extension, the audience settings need to the updated.

**Add the following settings**
- EDC_OAUTH_PROVIDER_AUDIENCE
- EDC_OAUTH_ENDPOINT_AUDIENCE

**Remove the following setting**
- EDC_IDS_ENDPOINT_AUDIENCE

Example
```
EDC_OAUTH_PROVIDER_AUDIENCE: idsc:IDS_CONNECTORS_ALL
EDC_OAUTH_ENDPOINT_AUDIENCE: http://plato-edc-controlplane:8282/api/v1/ids/data
```