# Control Plane Adapter Extension

The goal of this extension is to simplify the process of retrieving data out of EDC. It returns "EndpointDataReference" object, hiding all the details about contract offers, contract negotiation process and retrieving DataReference from EDC control-plane.

Additional requirements, that affects the architecture of the extension:
- can return data both in SYNC and ASYNC mode (currently only SYNC endpoint available)
- can be persistent, so that process can be restored from the point where it was before application was stopped (not implemented yet)  
- prepared to scale horizontally (not implemented yet, as EDC itself is not scalable)
- can retry failed part of the process

The simplified scenario, from the perspective of the client of the extension is as follows:
1. client sends a GET request with two parameters:
    * assetId
    * url of the provider control-plane
2. EndpointDataReference object is returned
3. client, using the DataReference, retrieves the Asset through data-plane

Example of the EndpointDataReference response:

```json
{
  "id": "ee8b758a-4b02-4cca-bb37-d0256b4638e7",
  "endpoint": "http://consumer-dataplane:9192/public",
  "authKey": "Authorization",
  "authCode": "eyJhbGciOiJSUzI1NiJ9.eyJkYWQiOi..................",
  "properties": {
    "cid": "1:b2367617-5f51-48c5-9f25-e30a7299235c"
  }
}
```

Diagram below shows the internal design of the extension:

![diagram](src/main/resources/control-plane-adapter.jpg)



TODO

* new, separate endpoint to return EndpointDataReference in ASYNCHRONOUS way
* persistence of the process, now it works in "in memory" mode
* query assets by type, not just by ID
* optional parameters, like:
  * request timeout
  * turning on/off contract negotiation result cache
  * ID of contract offer that should be used for negotiations
* DataReference error handling (now, if there is an error while waiting for EndpointDataReference notification, extension is not aware of it and process is stuck)
