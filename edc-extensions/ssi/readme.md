# EDC SSI-Extension

The SSI-Extension is an Identity Service which provides the possibility of connecting two EDCs with each other.
By presenting Claims as Verifiable Presentations which can be verified so that each other knows
who he is talking to without the need of logging in into another system.

## Verification Process

The process in which two EDCs connecting to each other is done by the ids:Multipart Controller,
in here we are concentrating of the token exchange and where the credentials come from.

### Usage 

For a example usage of the SSI extension, have a look on samples/06.0-ssi-authentication

### Limitations

The actual service don't validate the Token, this will be implemented as a Validation Service Endpoint.

### Sequence Diagram Consumer

![Consumer Sequence](ssi-doc/uml/data-transfer-ssi-consumer.png)

### Sequence Diagram Provider

![Consumer Sequence](ssi-doc/uml/data-transfer-ssi-provider.png)

### Class Flow Diagram

![Consumer Sequence](ssi-doc/uml/SSIExtensionClassDiagram.png)

