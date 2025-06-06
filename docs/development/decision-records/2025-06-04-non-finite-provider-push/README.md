# Non Finite Provider Push

## Decision

Provide explicit support for non-finite data transfers using the Provider-PUSH flow type.

## Rationale

Non-finite data is data that is defined by an infinite set or has no specified end. Currently, Provider-PUSH transfers transition the Transfer Processes and Data Flow to final states after the first data chunk is transferred, closing the communication channel between the Consumer and the Provider. This behavior prevents the exchange of non-finite data, as it incorrectly indicates that the transfer is complete, even though additional data will become available over time.

## Approach

The first step is to enable Providers to indicate the finiteness of their data. This will be done during Asset registration by explicitly declaring the data's finiteness in the data address.

The implemented solution will allow both finite and non-finite data to be transferred using the Provider-PUSH flow. Finite data transfers will remain unchanged. With non-finite data, Transfer Processes will continue indefinitely until either the Consumer or Provider explicitly terminates the transmission, therefore correctly reflects the state of the data transfer. The corresponding Data Flow will also remain active for the duration of the transfer.

The implemented solution will also include a triggering mechanism that enables the transfer of subsequent data chunks, allowing the active Transfer Processes and Data Flow to be reused. This mechanism will be exposed as a Provider-side endpoint on the Data Plane, enabling data transfers to be restarted on demand.
