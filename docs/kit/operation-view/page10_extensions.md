# EDC Extensions

The following extensions provide additional functionality to the core EDC.
They are currently only available in TractusX EDC.

### Business Partner Validation

This extension allows for validation of business partners after the creation of a contract.

### Control Plane Adapter

The goal of this extension is to simplify the process of retrieving data out of EDC.
It returns `EndpointDataReference` object, hiding all the communication details for contract offers,
contract negotiation process and retrieving `DataReference` from EDC control-plane.

### CX OAuth2

This extension enables OAuth2 authentication between EDC connectors,
instead of the more complex authentication flow used by core EDC.

### Data Encryption

The EDC encrypts sensitive information inside a token it sends to other applications (potentially cross-company).
This extension implements the encryption of this data and should be used with secure keys and algorithms at all times.

### Data Plane Selector

This control plane extension makes it possible configure one or more data plane instances.
During a transfer the control plane will look for an instance with matching capabilities to transfer data.

### Hashicorp Vault

The core EDC assumes secret storage via Azure Vault.
This extension allows for usage of Hashicorp Vault instead,
with the goal of platform agnosticism.

### PostrgreSQL Migration

While the core EDC is able to interact with PostgreSQL databases,
it does not automate migrations between schema versions.
This extension adds that functionality.

### Transfer Process SFTP

This extension allows for the use of SFTP backends for the data plane.

### S3 Data Transfer

TODO: Check for correct name and path
