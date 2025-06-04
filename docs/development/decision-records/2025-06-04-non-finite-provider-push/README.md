# Non Finite Provider Push

## Decision

Provide explicit support for non-finite data transfers using the Provider-PUSH flow type.

## Rationale

Non-finite data is data that is defined by an infinite set or has no specified end. Currently, Provider-PUSH transfers transition the Transfer Processes and Data Flow to final states after the first data chunk is transferred, closing the communication channel between the Consumer and the Provider. This behavior prevents the exchange of non-finite data, as it incorrectly indicates that the transfer is complete, even though additional data will become available over time.

## Approach

With non-finite data, Transfer Processes will continue indefinitely until either the Consumer or Provider explicitly terminates the transmission. By doing so, the Transfer Process correctly reflects the state of the data transfer. The implemented solution will allow both finite and non-finite data to be transferred using the Provider-PUSH flow. Additionally, a triggering mechanism is required to enable the transfer of subsequent data chunks.

1. Add an "isNonFinite" property to the `Asset`'s `DataAddress`:
    - The `DataAddress` is schemaless, so it can already receive any custom property
    - This property will indicate the data's finiteness and guide the `DataFlow` lifecycle.

2. Perform the data transfer:
    - Add a new extension that implements `PipelineService`, supporting both finite and non-finite data transfers.
        - The registration of `DataSourceFactory` and `DataSinkFactory` remains the same as in the existing `PipelineServiceImpl`.
        - The `transfer` method executes the data transfer as usual, and then evaluates the "isNonFinite" property:
            - If the property is present, set to `true` and the transfer succeded, a `CompletableFuture` that never completes is returned, keeping the Data Flow *STARTED*.
            - If the property is present, set to `true` but the transfer failed, a completed `CompletableFuture` is returned with the failure result.
            - If the property is absent or set to `false`, the `CompletableFuture` produced by the data transfer is returned, preserving the current behavior for finite transfers.

3. Add a new dataflow API to the management API context:
    - This API will be available in the dataplane.
    - This API will provide an endpoint to trigger a data transfer by a dataflow ID.

4. Add a new `DataFlowService`:
    - The service will include a `trigger` method that accepts a dataflow ID.
    - This method will get the correspondant `DataFlow` and apply the following validations:
        - The `DataFlow` must be of *PUSH* flow type.
        - The `DataFlow` must contain the "isNonFinite" property.
        - The `DataFlow` must be in the *STARTED* state.
    - If all validations succeed, the `DataFlow` transitions to *RECEIVED*, restarting the data transfer.
