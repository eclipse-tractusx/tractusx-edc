# Software Operation View

## Introduction

The following documentation will guide you through the TractusX deployment.
You will be setting up multiple controllers and enabling communication between them.

::: TractusX EDC or Core EDC?

The following guide assumes the use of the TractusX EDC.
It includes the Core EDC with all of its functionality.
However, this core is supplemented by extensions that allow for the use of additional backends and connection types.
Furthermore, the provided Helm charts, build configuration and tests allow for a smoother deployment.
:::

## Connector Components

In a usual EDC environment, each participant would operate at least one connector.
Each of these connectors consists of a control plane and a data plane.
The control plane functions as administration layer and is responsible for resource management, contract negotiation and administering data transfer.
The data plane does the heavy lifting of transferring and receiving data streams.

Each of these planes comes in several variants, allowing for example secrets to be stored in Azure Vault or a Hashicorp Vault.
The setup on the following pages assumes the use of Hashicorp Vault for secrets and PostgreSQL for data storage.
