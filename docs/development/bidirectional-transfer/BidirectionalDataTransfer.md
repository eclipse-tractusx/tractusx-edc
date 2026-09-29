# Overview: Endpoint Topologies

Bidirectional data transfers involve transmissions that can be sent by either the provider or consumer during the
transfer's lifetime. The provider sends data over a forward channel, while the client uses a response channel to send
data related to the forward transmission. For example, a provider sends parts data over the forward channel, while the
consumer sends data related to errors in the forward transmission via the response channel.

Bidirectional data transfers should be modeled using a single Dataspace Protocol *offer* and *contract agreement*. In
other words, a single offer represents the ability to send both forward and response messages, while an active contract
agreement can be used to initiate the transfer.

Bidirectional flows can be implemented using a variety of wire protocols, for example, HTTP or a messaging layer.
However, all scenarios correspond to one of two endpoint topologies:

- The consumer offers the forward channel endpoint, and the provider offers the response channel endpoint.
- The provider offers both the forward and response channel endpoints.

The Dataspace Protocol (DSP) defines two categories of data transfer: *push* and *pull*. The endpoint topologies
correlate to these categories as follows:

| Provider Push                                                                               | Consumer Pull                                              |
|---------------------------------------------------------------------------------------------|------------------------------------------------------------|
| Consumer offers the forward channel endpoint; provider offers the response channel endpoint | Provider offers the forward and response channel endpoints |

**In each case, the provider always offers the response channel.**

## The Data Plane

The Data Plane establishes data transfer communication channels and endpoints using a *wire protocol*. There are many
ways to do this, two of which are described below.

**HTTP Endpoints**

The forward and response channels are separate endpoints. The endpoints may be static, where all messages in a
particular direction are sent to the same endpoint, which then uses a correlation mechanism to process them, for
example, `https://test.com/forwardChannel` and `https//test.com/responseChannel`. Or, the endpoints may be dynamic,
where a path part contains a correlation ID, for example, `https://test.com/transferId/forwardChannel`
and `https://test.com/transferId/responseChannel`.

**Queues and Pub/Sub**

In this scenario, the forward channel is a *queue* or a pub/sub *topic* while the response channel is a *queue*. This is
a typical architecture used when designing systems with Message-Oriented-Middleware.

### Required Changes to the Data Plane Framework

The required changes to the Data Plane Framework to support bidirectional data transfers are minimal.

#### Response Endpoint `DataAddress`

The `DataAddress` in the `DataFlowResponseMessage` must contain a `https://w3id.org/edc/v0.0.1/ns/responseChannel`
property of type `DataAddress`. This `DataAddress` follows the same format as the outer `DataAddress` and represents the
response channel endpoint. For example, it may contain authorization data the consumer uses to access the response
channel endpoint.

#### The DataPlaneManager

The `DataPlaneManagerImpl` and its collaborators will need to be refactored to generate response
channel `DataAddresses`:

- `DataPlaneManagerImpl` must be modified to return an EDR in the case of a provider PUSH. This EDR will only contain
  a `https://w3id.org/edc/v0.0.1/ns/responseChannel` entry. The manager will delegate to `DataPlaneAuthorizationService`
  to generate the response.
- `DataPlaneAuthorizationServiceImpl` must be enhanced to support `responseChannel` generation. This should be keyed off
  of the transfer type. As part of this process, a `DataPlaneAuthorizationServiceImpl.createEndpointDataReference` must
  generate a `responseChannel` endpoint by delegating to a new
  method `PublicEndpointGeneratorService.generateCallbackFor(sourceDataAddress).` Access Tokens can be generated
  from `DataPlaneAccessTokenService`.

#### Technical Considerations

The above changes can work with both DSP pull and push scenarios. However, it is important to note a potential race
condition that could be introduced in PUSH transfers. Namely, provider-pushed data could potentially arrive before the
DSP start message containing the response channel `DataAddress` is received by the client. This is due to the nature of
asynchronous communications. In this case, the client would either need to skip sending a response or store the response
messages to send when it receives the response channel `DataAddress`.

The response channel lifetime is tied to the forward channel. For example, when the forward channel is closed, the
response channel will also be closed.

## Catena-X Standardization and Tractus-X Support

To achieve interoperability, Catena-X would need to standardize a bidirectional transfer type similar to its support of
HTTP push/pull and S3 types. This could then be implemented in Tractus-X EDC.

