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

CLIENT_SECRET=$(openssl rand -base64 16)
AES_KEY=$(echo aes_enckey_test | base64)
echo "$AES_KEY" > aes.key
echo "$CLIENT_SECRET" > client.secret

# add a "postStart" command to the vault config, that creates a oauth client secret and an aes-keys secret
yq -i ".vault.server.postStart |= [\"sh\",\"-c\",\"{\nsleep 5\n
/bin/vault kv put secret/client-secret content=$CLIENT_SECRET\n
/bin/vault kv put secret/aes-keys content=$AES_KEY\n}\"]" "$VALUES_FILE"