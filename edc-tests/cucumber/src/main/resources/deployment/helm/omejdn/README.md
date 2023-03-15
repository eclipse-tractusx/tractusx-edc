# Omejdn DAPS

This chart deployes an [IDS Omejdn DAPS](https://github.com/Fraunhofer-AISEC/omejdn-server).

Two Eclipse Dataspace Connectors need to be registered at the same DAPS instance, to be able to talk to each other. Each connector is registered in the DAPS by an unique client ID and a correpsonding client certificate.

New connectors are configured in the omejdn _values.yaml_.

In each Eclipse Dataspace Connector configure the following properties to use the DAPS.

```properties
    edc.oauth.client.id=<client ID from omejdn values.yaml>

    edc.oauth.provider.jwks.url="http://<name>:4567/.well-known/jwks.json"
    edc.oauth.token.url="http://<name>:4567/token"

    edc.oauth.private.key.alias=<key vault alias of certificate private key>
    edc.oauth.public.key.alias=<key vault alias of certificate configured in omejdn values.yaml>

    edc.oauth.provider.audience=idsc:IDS_CONNECTORS_ALL
```
