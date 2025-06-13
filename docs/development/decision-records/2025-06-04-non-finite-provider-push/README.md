# Non Finite Provider Push

## Decision

Provide explicit support for non-finite data transfers using the Provider-PUSH flow type.

## Rationale

Non-finite data is data that is defined by an infinite set or has no specified end. Currently, Provider-PUSH transfers transition the Transfer Processes and Data Flow to final states after the first data chunk is transferred, closing the communication channel between the Consumer and the Provider. This behavior prevents the exchange of non-finite data, as it incorrectly indicates that the transfer is complete, even though additional data will become available over time.

## Approach

The first step is to enable Providers to indicate the finiteness of their data. This will be done during Asset registration by explicitly declaring the data's finiteness in the data address as a new property named `isNonFinite`.

The implemented solution will provide an implementation of the `PipelineService` that allow both finite and non-finite data to be transferred using the Provider-PUSH flow. The behavior for finite data transfers will remain unchanged. With non-finite data, Transfer Processes will continue indefinitely until either the Consumer or Provider explicitly terminates the transmission, therefore correctly reflecting the state of the data transfer. This will be possible by guaranteeing that the `transfer` method returns a `CompletableFuture` that never completes in case of a successful, non-finite transfer.

The implemented solution will also include a triggering mechanism that enables the transfer of subsequent data chunks, allowing the active Transfer Processes and Data Flow to be reused. This mechanism will be exposed as a Provider-side endpoint on the Data Plane, enabling data transfers to be restarted on demand. A new `DataFlowApi` holding this endpoint will be added to the management API context. Additionally, a new `DataFlowService` will also be created, handling the logic to enable triggering a data transfer. 
