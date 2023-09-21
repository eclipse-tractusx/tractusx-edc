#!/bin/bash

output_dir=$1
repo_root=$2
release_version=$3

echo $1 $2 $3

cd ${repo_root}

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/
rsync -a --prune-empty-dirs --exclude 'build' --include '*' ./docs/kit/* ${output_dir}/tractusx-edc/Connector\ Kit/

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/03_deployment_via_helm
cp ./docs/samples/example-dataspace/README.md ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/03_deployment_via_helm/00_example_dataspace.md
cp ./docs/samples/edr-api-overview/edr-api-overview.md ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/03_deployment_via_helm/01_edr_api_overview.md

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/04_Test\ Your\ Setup
cp ./docs/development/postman/README.md ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/04_Test\ Your\ Setup/00_postman.md
cp ./docs/samples/Transfer\ Data.md ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/04_Test\ Your\ Setup/02_transfer_data.md
cp -r ./docs/samples/diagrams ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/04_Test\ Your\ Setup/

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/05_Migration
cp ./docs/migration/* ${output_dir}/tractusx-edc/Connector\ Kit/Operation\ View/05_Migration/

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View
cp ./docs/README.md ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/00_tractusx_edc.md
cp ./core/edr-core/README.md ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/06_edr_core.md
cp ./docs/development/Release.md ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/07_release.md

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/03_EDC\ Controlplane
rsync -a --prune-empty-dirs --include '*/' --exclude 'build' --include '*.md' --include '*.png' --include '*.puml' --exclude '*' ./edc-controlplane/* ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/03_EDC\ Controlplane

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/04_EDC\ Dataplane
rsync -a --prune-empty-dirs --include '*/' --exclude 'build' --include '*.md' --include '*.png' --include '*.puml' --exclude '*' ./edc-dataplane/* ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/04_EDC\ Dataplane

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/05_EDC\ Extensions
rsync -a --prune-empty-dirs --include '*/' --exclude 'build' --include '*.md' --include '*.png' --include '*.puml' --exclude '*' ./edc-extensions/* ${output_dir}/tractusx-edc/Connector\ Kit/Development\ View/05_EDC\ Extensions

mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Documentation
cp ./docs/development/coding-principles.md ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/02_coding_principles.md
cp ./pr_etiquette.md ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/03_pr_etiquette.md
cp ./styleguide.md ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/04_styleguide.md
mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/resources
cp ./resources/tx-checkstyle-config.xml ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/resources
cp ./SECURITY.md ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/05_security.md
mkdir -p ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/resources
cp ./resources/save_actions_scr.png ${output_dir}/tractusx-edc/Connector\ Kit/Documentation/resources/

curl https://api.swaggerhub.com/apis/tractusx-edc/${release_version}/swagger.yaml > ${output_dir}/tractusx-edc/tractusx-edc-${release_version}.yaml

cd ${output_dir}/tractusx-edc
zip -r ../tractusx-edc-docusaurus-${release_version}.zip ./*