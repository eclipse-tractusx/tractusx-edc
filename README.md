# Catena-X specific edc apps

This project provides pre-built Control-Plane and Data-Plane [docker](https://www.docker.com/) images and [helm](https://helm.sh/) charts of the [Eclipse DataSpaceConnector Project](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

## Inventory

The eclipse data space connector is split up into Control-Plane and Data-Plane, whereas the Control-Plane functions as administration layer
and has responsibility of resource management, contract negotiation and administer data transfer. 
The Data-Plane does the heavy lifting of transferring and receiving data streams.

Depending on your environment there are different derivatives of the control-plane prepared:

* [edc-controlplane-cosmosdb](edc-controlplane/edc-controlplane-cosmosdb)
* [edc-controlplane-memory](edc-controlplane/edc-controlplane-memory)
* [edc-controlplane-postgresql](edc-controlplane/edc-controlplane-postgresql)

Derivatives of the Data-Plane can be found here

* [edc-dataplane](edc-dataplane)

## Prerequisites

#### EDC artifacts

Since the [EDC](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) does 
not yet publish artifacts to a maven repository, which this project relies on, it needs 
to be built upfront to be used:

```shell
git submodule update --init
cd edc && ./gradlew publishToMavenLocal
```

## Build

```shell
./mvnw package -Pwith-docker-image
```
