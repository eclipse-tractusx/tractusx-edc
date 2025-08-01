# Non-Finite Provider Push

Non-finite data is data that is defined by an infinite set or has no specified end. Data sources that produce non-finite 
data will continue producing new data chunks over their lifetime.

While the **PULL** transfer model natively supports the transfer of non-finite data, **PUSH** transfers require special
consideration.

Typical PUSH transfer complete the Transfer Processes after sending the data that is currently available in the data 
source. This behavior is incompatible with non-finite data, as only the existing chunk at the time of transfer would be 
delivered.

The non-finite provider push mechanism permits the exchange of non-finite data in case additional data becomes available 
over time.

# Identifying Non Finite Data

Assets can identify data sources that produce non-finite data. To do this, the Asset should be registered with the
`isNonFinite` data address property set to `true`. For example, the following example request body registers an `HttpData` 
Asset that points to a non-finite data source:

```json
{
	"@context": {},
	"@type": "Asset",
	"@id": "{{ASSET_ID}}",
	"dataAddress": {
		"@type": "DataAddress",
		"type": "HttpData",
		"isNonFinite": "true",
		"baseUrl": "{{BASE_URL}}"
	}
}
```

# Transferring Non-Finite Data

To start a non-finite Provider-PUSH transfer simply initiate a Transfer Process with flow type PUSH to an non-finite 
Asset. Once the transfer starts data will eventually arrive at the data destination, similarly to usual PUSH transfers.

However, checking the Transfer Processes states after the transfer succeeded shows that they remain in the *STARTED* state, 
unlike typical transfers which transition to *COMPLETED*. This indicates that the transfer is still ongoing and has not yet 
concluded.

At this stage, the Data Provider can initiate additional transfers on demand by invoking the `DataFlow` API. This API is
available within the management API context and is exposed through the data plane. To trigger a new data transfer, execute 
the following request:

```http request
POST /v4alpha/dataflows/{{TRANSFER_PROCESS_ID}}/trigger HTTP/1.1
Host: https://provider-data.plane/api/management
X-Api-Key: password
Content-Type: application/json
```

The new contents of the data source will now be transferred again to the data destination. The Transfer Processes will 
remain in the *STARTED* state, allowing this trigger to be executed multiple times as new data becomes available.

The Connector does not calculate the difference between what has been sent and the new data to transfer. All data present 
on the data source at the time of the transfer will be pushed to the Consumer.

To close the communications channel, either the Consumer or the Provider must manually terminate the Transfer Process.
