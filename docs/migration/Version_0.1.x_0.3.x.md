# Migration from 0.1.x to 0.3.x

## Deprecation

- the `edc-controlplane` and `edc-dataplane` charts are deprecated, please use `tractusx-connector` which combines the former ones

## Management API changes

Details at the [official documentation on swaggerhub](https://app.swaggerhub.com/apis/eclipse-edc-bot/management-api/0.0.1-SNAPSHOT)

- Management API for creating resources (assets, policydefinitions, contractdefinitions, ...) will return a body containing the id of the created resource
- Added a `POST /request` for every management endpoint (assets, policydefinitions, ...) to query all the resources. The existent `GET /` have been deprecated
- added `id` field in `DataRequestDTO` (`/transferprocess` management api), if not set it will use a randomly generated one.
- Removed field `assetId` from `ContractOffer`. It was always null though, so there should be nothing to do about it.
- on `POST /contractdefinitions` a `duration` field can be added to control the duration of the contract.
- added the `GET /assets/{id}/address` endpoint to being able to retrieve the stored `DataAddress`

## Settings changes

- refactored the HTTP server contexts (more details on the [related decision record](https://github.com/eclipse-edc/Connector/blob/v0.1.0/docs/developer/decision-records/2022-11-09-api-refactoring/renaming.md)). They need to be refactored as:
  - `web.http.data` becomes `web.http.management`
  - `web.http.ids` becomes `web.http.protocol`
  - `web.http.validation`, `web.http.controlplane` and `web.http.dataplane` become `web.http.control`
- Healthcheck api now it's exposed under the `management` context.
- Removed default value for setting `edc.transfer.proxy.token.verifier.publickey.alias` so it must be valued accordingly
- made the state machine settings configurable so it will be possible to tune them accordingly. More details in the [related documentation entry](https://github.com/eclipse-edc/Connector/blob/v0.1.0/docs/developer/performance-tuning.md).
- renamed `edc.receiver.http.endpoint` to `edc.receiver.http.dynamic.endpoint`
- renamed `edc.oauth.public.key.alias` setting to `edc.oauth.certificate.alias`

## Other changes

- Supported `/public` data plane endpoint without trailing slash, that can be eventually removed from the configuration
- packages name changed from `org.eclipse.dataspaceconnector` to `org.eclipse.edc`
- To specify multiple BPN into Policies the operator `OR` can be used. More details in the [business-partner-validation extension documentation](../../edc-extensions/business-partner-validation)
- HTTP Dynamic Endpoint Data Reference: The URL for Endpoint Data Reference can be also provided via the call for starting the transfer process. More details [Http Dynamic EDR receiver](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/transfer/transfer-pull-http-dynamic-receiver)

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2022,2023 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
