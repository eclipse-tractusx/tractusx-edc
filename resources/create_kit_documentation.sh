#!/bin/bash

output_dir=$1
repo_root=$2
release_version=$3

echo $1 $2 $3

cd ${repo_root}

connector_kit="${output_dir}/tractusx-edc/Connector Kit"
mkdir -p "${connector_kit}"
rsync -a --prune-empty-dirs --exclude 'build' --include '*' ./docs/kit/* "${connector_kit}/"

operation_view="${connector_kit}/Operation View"
mkdir -p "${operation_view}/03_deployment_via_helm"
cp ./docs/samples/example-dataspace/README.md "${operation_view}/03_deployment_via_helm/00_example_dataspace.md"
cp ./docs/samples/edr-api-overview/edr-api-overview.md "${operation_view}/03_deployment_via_helm/01_edr_api_overview.md"

mkdir -p "${operation_view}/04_Test Your Setup"
cp ./docs/development/postman/README.md "${operation_view}/04_Test Your Setup/00_postman.md"
cp ./docs/samples/Transfer\ Data.md "${operation_view}/04_Test Your Setup/02_transfer_data.md"
cp -r ./docs/samples/diagrams "${operation_view}/04_Test Your Setup/"

mkdir -p "${operation_view}/05_Migration"
cp ./docs/migration/* "${operation_view}/05_Migration/"

development_view="${connector_kit}/Development View"
mkdir -p "${development_view}"
cp ./docs/README.md "${development_view}/00_tractusx_edc.md"
cp ./core/edr-core/README.md "${development_view}/06_edr_core.md"
cp ./docs/development/Release.md "${development_view}/07_release.md"

mkdir -p "${development_view}/03_EDC Controlplane"
rsync -a --prune-empty-dirs --include '*/' --exclude 'build' --include '*.md' --include '*.png' --include '*.puml' --exclude '*' ./edc-controlplane/* "${development_view}/03_EDC Controlplane"

mkdir -p "${development_view}/04_EDC Dataplane"
rsync -a --prune-empty-dirs --include '*/' --exclude 'build' --include '*.md' --include '*.png' --include '*.puml' --exclude '*' ./edc-dataplane/* "${development_view}/04_EDC Dataplane"

mkdir -p "${development_view}/05_EDC Extensions"
rsync -a --prune-empty-dirs --include '*/' --exclude 'build' --include '*.md' --include '*.png' --include '*.puml' --exclude '*' ./edc-extensions/* "${development_view}/05_EDC Extensions"
cp ./build/tractusx-edc.md "${development_view}/05_EDC Extensions/01_autodoc_manifest.md"

documentation="${output_dir}/tractusx-edc/Connector Kit/Documentation"
mkdir -p "${documentation}"
cp ./docs/development/coding-principles.md "${documentation}/02_coding_principles.md"
cp ./pr_etiquette.md "${documentation}/03_pr_etiquette.md"
cp ./styleguide.md "${documentation}/04_styleguide.md"
mkdir -p "${documentation}/resources"
cp ./resources/tx-checkstyle-config.xml "${documentation}/resources"
cp ./SECURITY.md "${documentation}/05_security.md"
mkdir -p "${documentation}/resources"
cp ./resources/save_actions_scr.png "${documentation}/resources/"

curl "https://api.swaggerhub.com/apis/tractusx-edc/${release_version}/swagger.yaml" > "${output_dir}/tractusx-edc/tractusx-edc-${release_version}.yaml"

cd ${output_dir}/tractusx-edc
zip -r ../tractusx-edc-docusaurus-${release_version}.zip ./*
