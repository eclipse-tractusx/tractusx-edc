# Non Finite Provider Push

## Decision

Provide explicit support for non-finite data transfers using the Provider-PUSH flow type.

## Rationale

Non-finite data is data that is defined by an infinite set or has no specified end.

Currently, Provider-PUSH transfers transition the Transfer Processes and Data Flow to final states after the data specified by the source `DataAddress` is transferred, closing the communication channel between the Consumer and the Provider.

This behavior needs to be adapted to permit the exchange of non-finite data, in case additional data becomes available over time.

## Approach

This feature consists of three main parts:

- Identify Non-Finite Data Flows;
- Handle Non-Finite Data During Transfers;
- Trigger Additional Data Transfers.

### Identify Non-Finite Data Flows

A new `FinitenessEvaluator` interface will be added exposing the methods `isNonFinite(DataFlow dataflow)` and `isNonFinite(DataFlowStartMessage message)`. This model allows for custom implementations of ways to identify non-finite data.

A default implementation will also be available, enabling Providers to indicate the finiteness of their data during Asset registration by setting a new property, `isNonFinite`, in the data address.

By default, data will be considered finite and will only be treated as non-finite if `isNonFinite` is set to `true`.

The following sections will describe how the `FinitenessEvaluator` will be used.

### Handle Non-Finite Data During Transfers

With non-finite data, Transfer Processes will stay in the *STARTED* state until explicitly terminated or suspended by either the Consumer or Provider.

Additionally, the implemented solution will also keep the corresponding Data Flow in the *STARTED* state after a data transfer, thereby preventing the completion of the Transfer Process.

To achieve this, a new implementation of the `PipelineService` will be provided, allowing both finite and non-finite data to be transferred using the Provider-PUSH flow. 

This implementation will be similar to upstream's `PipelineServiceImpl`, with the following changes to the `transfer` method:

- For finite data transfer, return a `CompletableFuture` with the transfer result (current behavior);
- For failed non-finite data transfer, return a `CompletableFuture` with the failed transfer result;
- For successful non-finite data transfer, return a `CompletableFuture` that never completes, preventing subsequent state transitions.

```java
// Example implementation
public CompletableFuture<StreamResult<Object>> transfer(DataFlowStartMessage request, DataSink sink) {
    ...

    var futureTransferResult = sink.transfer(source)
            .thenApply(result -> {
                terminate(request.getProcessId());
                return result;
            });

    // Keep current behavior for finite data transfers
    if (!finitenessEvaluator.isNonFinite(request)) {
        return futureTransferResult;
    }

    // Complete transfer only if transfer result failed for non-finite data
    var returnedFuture = futureTransferResult.newIncompleteFuture();
    futureTransferResult.thenAccept(result -> {
        if (result.failed()) {
            returnedFuture.complete(result);
        }
    });
    return returnedFuture;
}
```

The private `terminate` method will also be updated to account for the suspension and termination of non-finite data transfers.

Since the associated `DataSource` reference is removed from the references map after a data transfer finishes, avoiding memory leaks, future suspension or termination request might not find it.

This method will be updated to return a successful `StreamResult` even if the source was not found.

```java
private StreamResult<Void> terminate(String dataFlowId) {
    var source = sources.remove(dataFlowId);
    if (source != null) {
        try {
            source.close();
        } catch (Exception e) {
            return StreamResult.error("Cannot terminate DataFlow %s: %s".formatted(dataFlowId, e.getMessage()));
        }
    }
    return StreamResult.success();
}
```

### Trigger Additional Data Transfers

The implemented solution will also include a triggering mechanism that enables the transfer of subsequent data chunks, allowing the active Transfer Processes and Data Flow to be reused.

This mechanism will be based on the `PipelineService` transfer behavior, retrieving data from the defined source data address and transfering it to the defined data destination.

The trigger mechanism will be exposed as a Provider-side endpoint on the Data Plane, allowing data transfers to be restarted on demand.

A new `DataFlowApi` and its corresponding controller will be added to the management API context. This API will expose a "trigger endpoint" at `POST /api/v1/dataflows/{dataflowId}/trigger`.

When invoked, this endpoint will call a new component, the `DataFlowService`, responsible for handling the logic to trigger a data transfer. The `DataFlowService` will contain a `trigger` method that applies the following validations:

- The `dataflowId` must map to an existing `DataFlow`, otherwise returns `NOT_FOUND`;
- The `DataFlow` must be of PUSH flow type, otherwise returns `BAD_REQUEST`.
- The `DataFlow` must be non-finite, otherwise returns `BAD_REQUEST`.
- The `DataFlow` must be in the *STARTED* state, otherwise returns `CONFLICT`.

If all validations succeed, the `DataFlow` transitions to *RECEIVED*, restarting the data transfer.
