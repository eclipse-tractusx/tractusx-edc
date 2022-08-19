# Data Plane Selector Configuration Exception

This control-plane extension makes it possible configure one or more Data Plane Instances. During a transfer the control
plane will look for an instance with matching capabilities to transfer data.

## Configuration

Per data plane instance the following settings must be configured. As `<data-plane-id>` any unique string is valid.

| Key                                                     | Description                                       | Mandatory | Example                                                           |
|:--------------------------------------------------------|:--------------------------------------------------|-----------|-------------------------------------------------------------------|
| edc.dataplane.selector.<data-plane-id>.url              | URL to connect to the Data Plane Instance.        | X         | http://plato-edc-dataplane:9999/api/dataplane/control             |
| edc.dataplane.selector.<data-plane-id>.sourcetypes      | Source Types in a comma separated List.           | X         | HttpData                                                          |
| edc.dataplane.selector.<data-plane-id>.destinationtypes | Destination Types in a comma separated List.      | X         | HttpProxy                                                         |
| edc.dataplane.selector.<data-plane-id>.properties       | Additional properties of the Data Plane Instance. | (X)       | { "publicApiUrl:": "http://plato-edc-dataplane:8185/api/public/" } |

The property `publicApiUrl` is mandatory for Data Plane Instances with destination type `HttpProxy`.

**Helm Example Configuration using environment variables**
```yaml
EDC_DATAPLANE_SELECTOR_PLATOPLANE_URL: http://plato-edc-dataplane:9999/api/dataplane/control
EDC_DATAPLANE_SELECTOR_PLATOPLANE_SOURCETYPES : HttpData
EDC_DATAPLANE_SELECTOR_PLATOPLANE_DESTINATIONTYPES: HttpProxy
EDC_DATAPLANE_SELECTOR_PLATOPLANE_PROPERTIES: >-
  { 
    "publicApiUrl": "http://plato-edc-dataplane:8185/api/public"
  }
```