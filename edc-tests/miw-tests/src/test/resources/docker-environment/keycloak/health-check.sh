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

# health check approach taken from https://stackoverflow.com/a/75693900/7079724

exec 3<>/dev/tcp/localhost/8080

echo -e "GET /health/ready HTTP/1.1\nhost: localhost:8080\n" >&3

timeout --preserve-status 1 cat <&3 | grep -m 1 status | grep -m 1 UP
ERROR=$?

exec 3<&-
exec 3>&-

exit $ERROR