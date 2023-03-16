# Migration from 0.1.x to 0.3.x


## Management API changes

details at the [official documentation on swaggerhub](https://app.swaggerhub.com/apis/eclipse-edc-bot/management-api/0.0.1-SNAPSHOT)

- Management API for creating resources (assets, policydefinitions, contractdefinitions, ...) will return a body containing the id of the created resource
- Added a `POST /request` for every management endpoint (assets, policydefinitions, ...) to query all the resources. The existent `GET /` have been deprecated
- added `id` field in `DataRequestDTO` (`/transferprocess` management api), if not set it will use a randomly generated one.
- Removed field `assetId` from `ContractOffer`. It was always null though, so there should be nothing to do about it.
- on `POST /contractdefinitions` a `duration` field can be added to control the duration of the contract.
- added the `GET /assets/{id}/address` endpoint to being able to retrieve the stored `DataAddress`
- concerning the Business Partner Validation Extension a so called orconstraint must be used instead of an IN operator. More details in the [related documentation entry](https://github.com/catenax-ng/product-edc/tree/0.3.0/edc-extensions/business-partner-validation)

## Settings changes
- Removed default value for setting `edc.transfer.proxy.token.verifier.publickey.alias` so it must be valued accordingly
- made the state machine settings configurable so it will be possible to tune them accordingly. More details in the [related documentation entry](https://eclipse-edc.github.io/docs/#/submodule/Connector/docs/developer/performance-tuning).
- refactored the HTTP server contexts, they can be configured with the settings group `web.http.management`, `web.http.control`, `web.http.protocol`, `web.http.public`. More details on the [related decision record](https://eclipse-edc.github.io/docs/#/submodule/Connector/docs/developer/decision-records/2022-11-09-api-refactoring/renaming)
- renamed `edc.oauth.public.key.alias` setting to `edc.oauth.certificate.alias`

## Other changes:
- Supported `/public` data plane endpoint without trailing slash, that can be eventually removed from the configuration
- packages name changed from `org.eclipse.dataspaceconnector` to `org.eclipse.edc`
