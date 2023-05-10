#!/bin/bash

#
# Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0
#
# SPDX-License-Identifier: Apache-2.0
#
# Contributors:
#       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
#
#

set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "usage prepare-test.sh PATH_TO_YAML"
  echo ""
  echo "Please provide the path to the YAML file, which contains the config for the test infrastructure! In most cases
  this will be edc-tests/deployment/src/main/resources/helm/test-infrastructure/values.yaml"
  exit 42
fi

VALUES_FILE=$1
KEY_FILE=daps.key
CERT_FILE=daps.cert

# generate a new short-lived certificate and export the private key
openssl req -newkey rsa:2048 -new -nodes -x509 -days 1 -keyout $KEY_FILE -out $CERT_FILE -subj "/CN=test"

DAPSCRT=$(cat $CERT_FILE)
DAPSKEY=$(cat $KEY_FILE)
AES_KEY=$( echo aes_enckey_test | base64)

# replace the cert for DAPS
yq -i ".idsdaps.connectors[0].certificate=\"$DAPSCRT\"" "$VALUES_FILE"

# add a "postStart" command to the vault config, that creates a daps-key, daps-cert and an aes-keys secret
yq -i ".vault.server.postStart |= [\"sh\",\"-c\",\"{\nsleep 5\n\ncat << EOF | /bin/vault kv put secret/daps-crt content=-\n$DAPSCRT\nEOF\n\n
cat << EOF | /bin/vault kv put secret/daps-key content=-\n$DAPSKEY\nEOF\n\n
/bin/vault kv put secret/aes-keys content=$AES_KEY\n\n}\"]" "$VALUES_FILE"