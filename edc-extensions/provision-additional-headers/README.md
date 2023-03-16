# Provision: additional headers

The goal of this extension is to provide additional headers to the request to the backend service done by the provider
in order to retrieve the data that will be given to the consumer.

This gives for example the provider backend service the possibility to audit the data requests.

The following headers are added to the `HttpDataAddress`:
- `Edc-Contract-Agreement-Id`: the id of the contract agreement
