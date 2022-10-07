# Data management API authentication using XSUAA Authentication Service

XSUAA authentication service rely on [OAuth 2.0](https://oauth.net/) protocol and OAuth 2.0 access tokens. Some platform which makes use of it are [SAP Cloud Platform](https://www.sap.com/products/cloud-platform.html), [SAP HANA XS Advanced](https://help.sap.com/viewer/4505d0bdaf4948449b7f7379d24d0f0d/2.0.00/en-US), ...

This extension makes use of opensource [Java-security client library](https://github.com/SAP/cloud-security-xsuaa-integration/tree/main/java-security) to verify the incoming token on data management API.

This extension could also serve as an implementation template for other OAuth 2.0 services.

**Please note** The service configuration values are loaded based on the respective environment: [(source)](https://github.com/SAP/cloud-security-xsuaa-integration/tree/main/java-security#setup-step-1-load-the-service-configurations)
```
OAuth2ServiceConfiguration serviceConfig = Environments.getCurrent().getXsuaaConfiguration();
```
**For local testing**, please provide the values for the following environment variables: `XSUAA_APP_ID`, `XSUAA_UAA_DOMAIN`, `XSUAA_AUTH_URL`, `XSUAA_CLIENT_ID`, `XSUAA_CLIENT_SECRET`

**In Kubernetes/Kyma environment**, please configure as explained below so that the client library could load the service configuration values [(source)](https://github.com/SAP/cloud-security-xsuaa-integration/tree/main/java-security#mega-service-configuration-in-kuberneteskyma-environment)

Library supports services provisioned by SAP BTP service-operator. To access service instance configurations from the application, Kubernetes secrets need to be provided as files in a volume mounted on application's container.
- BTP Service-operator up to v0.2.2 - Library will look up the configuration files in the following paths:
  - XSUAA: `/etc/secrets/sapbtp/xsuaa/<YOUR XSUAA INSTANCE NAME>`
  - IAS: `/etc/secrets/sapbtp/identity/<YOUR IAS INSTANCE NAME>`
    
- BTP Service-operator starting from v0.2.3 - Library reads the configuration from k8s secret that is stored in a volume, this volume's `mountPath` must be defined in environment variable `SERVICE_BINDING_ROOT`
  - `SERVICE_BINDING_ROOT` environment variable needs to be defined with value that points to volume mount's directory (`mounthPath`) where service binding secret will be stored. e.g.,
    ```
    containers:
    - image: <YOUR IMAGE REPOSITORY>
    env:
    - name: SERVICE_BINDING_ROOT
      value: "/bindings/"
    ```
  - upon creation of service binding, a kubernetes secret with the same name as the binding is created. This binding secret needs to be stored to pod's volume. e.g.,
    ```
    volumeMounts:
    - name: edc-provider-uaa
      mountPath: "/bindings/edc-provider-uaa"
      readOnly: true
    volumes:
    - name: edc-provider-uaa
      secret:
        secretName: edc-provider-uaa
    ```