#
#  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
#
#

#!/bin/bash

response=$(curl -X POST -d 'client_id=participant&grant_type=client_credentials&client_secret=secret&audience=did:example:participant&bearer_access_scope=org.eclipse.tractusx.vc.type:MembershipCredential:read' http://localhost:8990/v1/sts/token)
token=$(echo "$response" | jq -r '.access_token')

echo "SI_TOKEN=$token" >> "$GITHUB_ENV"


