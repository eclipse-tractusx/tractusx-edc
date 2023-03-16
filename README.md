<a name="readme-top"></a>

<!-- Project Shields -->
[![Contributors][contributors-shield]][contributors-url]
[![Stargazers][stars-shield]][stars-url]
[![Apache 2.0 License][license-shield]][license-url]
[![Latest Release][release-shield]][release-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/eclipse-edc/Connector">
    <img src="https://raw.githubusercontent.com/eclipse-edc/Connector/main/resources/media/logo.png" alt="Logo" width="80" height="80">
  </a>

  <h3 align="center">Product Eclipse Dataspace Connector</h3>
  <h4 align="center">Catena-X</h4>

  <p align="center">
    Container images and deployments of the Eclipse Dataspace Components open source project.
    <br />
    <a href="https://github.com/catenax-ng/product-edc/tree/main/docs"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/eclipse-edc/Connector">View Eclipse Dataspace Components</a>
    ·
    <a href="https://github.com/catenax-ng/product-edc/releases">Releases</a>
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

## About The Project

The project provides pre-built control- and data-plane [docker](https://www.docker.com/) images and [helm](https://helm.sh/) charts of the [Eclipse DataSpaceConnector Project](https://github.com/eclipse-edc/Connector).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Inventory

The eclipse data space connector is split up into Control-Plane and Data-Plane, whereas the Control-Plane functions as administration layer
and has responsibility of resource management, contract negotiation and administer data transfer. 
The Data-Plane does the heavy lifting of transferring and receiving data streams.

Depending on your environment there are different derivatives of the control-plane prepared:

* [edc-controlplane-memory](edc-controlplane/edc-controlplane-memory) with dependency onto
    * [Azure Key Vault](https://azure.microsoft.com/en-us/services/key-vault/#product-overview)
* [edc-controlplane-postgresql](edc-controlplane/edc-controlplane-postgresql) with dependency onto
    * [Azure Key Vault](https://azure.microsoft.com/en-us/services/key-vault/#product-overview)
    * [PostgreSQL 8.2 or newer](https://www.postgresql.org/)
* [edc-controlplane-postgresql-hashicorp-vault](edc-controlplane/edc-controlplane-postgresql-hashicorp-vault) with dependency onto
    * [Hashicorp Vault](https://www.vaultproject.io/)
    * [PostgreSQL 8.2 or newer](https://www.postgresql.org/)

Derivatives of the Data-Plane can be found here

* [edc-dataplane-azure-vault](edc-dataplane/edc-dataplane-azure-vault) with dependency onto
    * [Azure Key Vault](https://azure.microsoft.com/en-us/services/key-vault/#product-overview)
* [edc-dataplane-hashicorp-vault](edc-dataplane/edc-dataplane-hashicorp-vault) with dependency onto
    * [Hashicorp Vault](https://www.vaultproject.io/)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Getting Started

<p align="right">(<a href="#readme-top">back to top</a>)</p>


### Build

Build Product-EDC together with its Container Images
```shell
./gradlew dockerize
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## License

Distributed under the Apache 2.0 License. See [LICENSE](https://github.com/catenax-ng/product-edc/blob/main/LICENSE) for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/catenax-ng/product-edc.svg?style=for-the-badge
[contributors-url]: https://github.com/catenax-ng/product-edc/graphs/contributors
[stars-shield]: https://img.shields.io/github/stars/catenax-ng/product-edc.svg?style=for-the-badge
[stars-url]: https://github.com/catenax-ng/product-edc/stargazers
[license-shield]: https://img.shields.io/github/license/catenax-ng/product-edc.svg?style=for-the-badge
[license-url]: https://github.com/catenax-ng/product-edc/blob/main/LICENSE
[release-shield]: https://img.shields.io/github/v/release/catenax-ng/product-edc.svg?style=for-the-badge
[release-url]: https://github.com/catenax-ng/product-edc/releases
