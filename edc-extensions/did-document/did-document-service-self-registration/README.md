# did-document-service-self-registration

## Overview
This extension provides automatic self-registration and de-registration of a DID Document service entry at runtime.
It is designed to simplify service lifecycle management by registering a service entry on application startup and removing it on shutdown, based on configuration properties.

## Self Service Registration
When enabled, the extension registers a service entry with below details in the DID Document.

| Property        | Value                                                                                             |
|-----------------|---------------------------------------------------------------------------------------------------|
| id              | Value defined via config `tx.edc.did.service.self.registration.id`                                |
| type            | DataService                                                                                       |
| serviceEndpoint | The EDC's data space version endpoint (e.g., `https://<edc-host>/edc/.well-known/dspace-version`) |

## Features
- Automatically registers a DID Document service entry on startup.
- Automatically deletes the service entry on shutdown.
- Can be enabled or disabled via configuration property.
- No action is taken if the required implementation extension to update did document is not present.

## Configuration
The extension is controlled by the following configuration properties:

| Property                                       | Required | Default | Description                                                          |
|------------------------------------------------|----------|---------|----------------------------------------------------------------------|
| `tx.edc.did.service.self.registration.enabled` | false    | false   | Enables or disables self-registration extension.                     |
| `tx.edc.did.service.self.registration.id`      | false    | (none)  | The ID to use for service self-registration (should be a valid URI). |

> `tx.edc.did.service.self.registration.id` is required if self-registration is enabled (`tx.edc.did.service.self.registration.enabled=true`).

## Extension Lifecycle
- **On Startup:**
  - If enabled, the extension reads the service entry configuration and automatically registers the service in the DID Document using the configured client.
- **On Shutdown:**
  - The extension automatically deletes the registered service entry from the DID Document.

## Disable the Extension
- Set the property `tx.edc.did.service.self.registration.enabled=false` in your configuration to disable this feature.
- Do not include any extension which provides the implementation of SPI `DidDocumentServiceClient` to update the DID Document.
