<a name="readme-top"></a>

<!-- Project Shields -->
[![Contributors][contributors-shield]][contributors-url]
[![Stargazers][stars-shield]][stars-url]
[![Apache 2.0 License][license-shield]][license-url]
[![Latest Release][release-shield]][release-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/eclipse-dataspaceconnector/DataSpaceConnector">
    <img src="https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/raw/main/docs/_media/icon.png" alt="Logo" width="80" height="80">
  </a>

  <h3 align="center">Product Eclipse Dataspace Connector</h3>
  <h4 align="center">Catena-X</h4>

  <p align="center">
    Container images and deployments of the Eclipse Dataspace Connector open source project.
    <br />
    <a href="https://github.com/eclipse-tractusx/tractusx-edc/tree/feature/update-readme-md/docs"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/eclipse-dataspaceconnector/DataSpaceConnector">View Eclipse Dataspace Connector</a>
    ·
    <a href="https://github.com/eclipse-tractusx/tractusx-edc/releases">Releases</a>
    ·
    <a href="https://jira.catena-x.net/projects/A1IDSC/summary">Report Bug / Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
    </li>
    <li>
      <a href="#inventory">Inventory</a>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#build">Build</a></li>
      </ul>
    </li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

## All-In-One Deployment

The Product EDC Demo Deployment creates a complete, independent and already configured EDC test environment.

This deployment may function as

- reference setup for teams, that want to create their own connector
- standalone test environment to try different things out

### 1. Setup

Follow these steps to get a fully functional EDC demo environment out of the box.

Requirements: Install on your machine

- Minikube
  - Documentation <https://minikube.sigs.k8s.io/docs/start/>
- Helm
  - Documentation <https://helm.sh/docs/intro/install/>

Note: If you are planing to create your local EDC control plane, instead of downloading it as a Docker image, you 
can take a look at the instructions in the [eclipse-TractusX website](https://eclipse-tractusx.github.io/docs/kits/product-edc/docs/kit/operation-view/page03_local_setup_controlplane)

### 2. Start Demo Environment

#### Update Helm Dependencies

```bash
helm dependency update
```

#### Install Demo Chart

```bash
helm install edc-all-in-one --namespace edc-all-in-one --create-namespace .
```

This will deploy the following components:

![Deployed Components](./misc/diagrams/deployed_components.png)

### Stop Demo Environment

#### Uninstall Demo Chart

```bash
helm uninstall edc-all-in-one --namespace edc-all-in-one
```

### Components

Overview of the installed components.

#### EDC Control Plane

The EDC Control Plane does

- data/contract offering
- contract negotiation
- data transfer coordination

Two control planes always talk to each other using IDS messages. Therefore, when telling one connector to talk to
another connector, the target endpoint must point to the IDS API (e.g `http://[myTargetConnector].com/api/v1/ids`).

The connector owner should only talk to the control plane via the Data Management API. The API is not only used for
simple data management, but for initiating inter-connector communication as well.

#### EDC Data Plane

The EDC Data Plane is used for the actual data transfer.

At the time of writing the Data Plane may only function as HTTP proxy and does not support any other type of
transfer. Additional transfer capabilities could be added by including new EDC extensions in the Data Plane application.

#### PostgreSQL

This database is used to persist the state of the Control Plane.

#### HashiCorp Vault

The Control- and Data Plane will persist confidential in the vault and persist and communicate using only the secret
names.

#### Backend Application

After a Data Transfer is successfully prepared the control plane will contact the a configurable endpoint with the
information it needs to initiate the data transfer. This transfer flow, where something like a Backend Application is
required, is unique to the HTTP Proxy data transfer flow.

The Backend Application has an API endpoint, that is configured in the control plane. After it gets called with the data
transfer information, it will do the actual data transfer and store the data on disk.

#### Omejdn DAPS

Instead of the Catena-X DAPS this demo configures and deploys it's own DAPS instance.

### Testing transfer data

You can learn how to test a correct data transfer from your EDC using the following [Tractus EDC Data Transfer Link](https://eclipse-tractusx.github.io/docs/kits/product-edc/docs/samples/Transfer%20Data) 

## License

Distributed under the Apache 2.0 License. See [LICENSE](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/LICENSE) for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge
[contributors-url]: https://github.com/eclipse-tractusx/tractusx-edc/graphs/contributors
[stars-shield]: https://img.shields.io/github/stars/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge
[stars-url]: https://github.com/eclipse-tractusx/tractusx-edc/stargazers
[license-shield]: https://img.shields.io/github/license/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge
[license-url]: https://github.com/eclipse-tractusx/tractusx-edc/blob/main/LICENSE
[release-shield]: https://img.shields.io/github/v/release/eclipse-tractusx/tractusx-edc.svg?style=for-the-badge
[release-url]: https://github.com/eclipse-tractusx/tractusx-edc/releases
