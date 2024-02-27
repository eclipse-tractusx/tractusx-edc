#################################################################################
#  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#
#  See the NOTICE file(s) distributed with this work for additional
#  information regarding copyright ownership.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0.
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  License for the specific language governing permissions and limitations
#  under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#################################################################################

#!/bin/bash

response=$(curl -X POST -d 'client_id=miw_private_client&grant_type=client_credentials&client_secret=miw_private_client&scope=openid' http://localhost:8080/realms/miw_test/protocol/openid-connect/token)
token=$(echo "$response" | jq -r '.access_token')

credentials=$(curl --url 'http://localhost:8000/api/credentials?type=SummaryCredential' --header "Authorization: Bearer $token"  --header 'Content-Type: application/json'  | jq -r '.content')


vp_token=$(curl --request POST \
            --url 'http://localhost:8000/api/presentations?asJwt=true&audience=http://test' \
            --header "Authorization: Bearer $token" \
            --header 'Content-Type: application/json' \
            --data "{ \"verifiableCredentials\": $credentials }" \
            | jq -r '.vp')

echo "VP_TOKEN=$vp_token" >> "$GITHUB_ENV"


