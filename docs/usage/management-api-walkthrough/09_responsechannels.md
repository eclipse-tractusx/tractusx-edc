# Using transfer process response channels

In certain cases, a dataspace consumer might need to provide transfer process feedback to its provider counterpart.
This bidirectional exchange of information requires two separate channels: one forward channel where data flows in the
direction of the consumer from the provider, and a response channel (back channel) where data flows in the reverse
direction.
The EDC Connector enables such bidirectional flow of information.

As of writing of this document, a known workaround is in place to enable the same level of communication. The provider
creates an additional dataset offer, offering its consumer access to an endpoint where messages can be received via HTTP
POST requests. Albeit the creation of unnecessary offers, it is not advisable to use case architects setup their use
cases
in such manner, as per the dataspace reference architecture the flow of data should always happen in the direction of
the
consumer from the provider.

Using a transfer process response channel guarantees compliance and eliminates unnecessary dataset offers and all the
extra
steps needed to obtain access to the underlying endpoints.

## HTTP response channels

The HTTP wire protocol is the only supported protocol for response data communication as of now. Given the bellow
mentioned
considerations, and by following the bellow demonstrated process, a dataspace consumer can send response messages to a
provider.

## Considerations

- Considering the EDC Connector operator has enabled the HTTP response channel for a certain connector installation, by
  setting the following configuration:

```
edc.dataplane.api.public.response.baseurl
# A common practice is to use <DATAPLANE_PUBLIC_ENDPOINT>/responseChannel
```

- A `HttpData` type asset is offered by the provider, exposing the base url of the backend application.
- The backend application exposes an `/responseChannel` endpoint, where messages will be wired to by the dataplane.

## Getting an EDR with response channel properties

## Sending message to the response channel

## Current limitations

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: 2025 Contributors of the Eclipse Foundation
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)