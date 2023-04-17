# Supporting Infrastructure Deployment

The Supporting Infrastructure Deployment creates a complete, independent and already configured EDC test environment.
During the automated business tests, these infrastructure components are deployed together with two connectors (Plato & Sokrates).

This deployment could also be used as

- reference setup for teams, that want to create their own connector
- standalone infrastructure to try things out

This deployment should **never** be used

- in **any** production or near production environments
- in **any** long living internet facing connector setups

## Omejdn DAPS

The Dynamic Attribute Provisioning Service (DAPS) is a component of the IDS Ecosystem.
The Fraunhofer Institute has created a DAPS reference implementation, the Omejdn
DAPS ([link](https://github.com/Fraunhofer-AISEC/omejdn-server)). This deplyoment configures and deployes a instance of
this reference implementation.

Definition of DAPS from the IDS Reference architecture v3.0:

> The Identity Provider acts as an agent for the International
> Data Spaces Association. It is responsible for issuing technical identities to parties that have been approved to become
> Participants in the International Data Spaces. The Identity
> Provider is instructed to issue identities based on approved
> roles (e.g., App Store or App Provider). Only if equipped with
> such an identity, an entity is allowed to participate in the International Data Spaces

Also, please note, that the Omejdn DAPS is meant as research sandbox and should not be used in anq
productive environment.

> **IMPORTANT:** Omejdn is meant to be a research sandbox in which we can (re)implement standard protocols and
> potentially extend and modify functionality under the hood to support research projects. Use at your own
> risk! ([source](https://github.com/Fraunhofer-AISEC/omejdn-server))

## HashiCorp Vault

The Control- and Data Plane persist confidential in the vault and persist and communicate using only the secret
names. Hence, it is not possible to run a connector without an instance of a vault.

## PostgreSQL

This database is used to persist the state of the Control Plane.

## Setup

Simply execute the following comment in a shell:

```shell
helm install infra edc-tests/deployment/src/main/resources/helm/test-infrastructure --update-dependencies
```
