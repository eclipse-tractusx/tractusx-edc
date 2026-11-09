#!/usr/bin/env bash
#################################################################################
#  Copyright (c) 2026 Catena-X Automotive Network e.V.
#  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
#  AI Disclosure: This file was entirely AI-generated.
#  The AI-generated portions are made available under CC0-1.0 and not subject
#  to the project's license. The human contributor has reviewed and verified
#  that the code is correct.
#
#  SPDX-License-Identifier: Apache-2.0 AND CC0-1.0
#  Assisted-By: Claude Code (claude-opus-5-5)
#################################################################################

# Inventory of the HTTP endpoints Tractus-X EDC adds on top of upstream: every JAX-RS controller
# under edc-extensions, with method, full path, deprecation, the API context it is registered
# in, and whether a controller test and a walkthrough doc exist.
#
# Usage: api-inventory.sh [path-regex]      e.g. api-inventory.sh edrs   |   api-inventory.sh 'v4alpha'
set -euo pipefail

ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
FILTER="${1:-.}"

printf '%-7s %-52s %-38s %-11s %-5s %-4s %s\n' METHOD PATH CONTROLLER CONTEXT TEST DOC NOTES
for f in $(git grep -l -E '^@Path\(' -- 'edc-extensions/**/src/main/**/*.java' 2>/dev/null); do
  cls=$(basename "$f" .java)
  # API context: find where the controller is registered (registerResource(ApiContext.X / "x", new Cls...)
  ctx=$(git grep -h -E "registerResource\([^,]+, *new $cls([^A-Za-z0-9_]|$)" -- 'edc-extensions/**/src/main/**/*.java' 2>/dev/null \
        | sed -E 's/.*registerResource\(([^,]+),.*/\1/; s/ApiContext\.//; s/"//g' | head -1 | tr '[:upper:]' '[:lower:]' || true)
  # fallback: controller created into a variable or the call spans lines -> look around 'new <Controller>('
  [ -n "$ctx" ] || ctx=$(git grep -h -B3 -A3 -E "new $cls\(" -- 'edc-extensions/**/src/main/**/*.java' 2>/dev/null \
        | grep -oE 'registerResource\([^,]+' | head -1 | sed -E 's/registerResource\(//; s/ApiContext\.//; s/"//g' | tr '[:upper:]' '[:lower:]' || true)
  # a constant (e.g. API_CONTEXT, PUBLIC_API_CONTEXT): resolve its string value inside the module
  if echo "$ctx" | grep -E '^[a-z0-9_]*_[a-z0-9_]*$' >/dev/null; then
    const=$(echo "$ctx" | tr '[:lower:]' '[:upper:]')
    val=$(git grep -h -E "String $const *= *\"" -- "${f%%/src/main/*}/src/main" 2>/dev/null | sed -E 's/.*= *"([^"]*)".*/\1/' | head -1 || true)
    [ -n "$val" ] && ctx="$val"
  fi
  [ -n "$ctx" ] || ctx="?"
  mod=${f%%/src/main/*}
  base=$(sed -n 's/^@Path("\([^"]*\)").*/\1/p' "$f" | head -1)
  doc="no"; [ -n "$base" ] && git grep -F -e "$base" -- docs/usage >/dev/null 2>&1 && doc="yes"
  # tests are often shared across API versions (ConnectorDiscoveryControllerTest covers V3 and V4alpha)
  stem=$(echo "$cls" | sed -E 's/V[0-9]+(Alpha|Beta)?//; s/Controller$//')
  test="no"; git ls-files "$mod/src/test/*${cls}Test.java" "$mod/src/test/*${stem}*Test.java" | grep . >/dev/null && test="yes"
  awk -v cls="$cls" -v ctx="$ctx" -v test="$test" -v doc="$doc" '
    function flush() { if (verb != "") { p = base subp; gsub(/\/+/, "/", p); printf "%-7s %-52s %-38s %-11s %-5s %-4s %s\n", verb, p, cls, ctx, test, doc, (dep_cls || dep_m ? "DEPRECATED" : "") } verb = ""; subp = ""; dep_m = 0 }
    /^@Deprecated/ { dep_cls = 1 }
    /^@Path\("/ && !in_class { match($0, /"[^"]*"/); base = substr($0, RSTART + 1, RLENGTH - 2) }
    /^(public )?(abstract )?(class|interface) / { in_class = 1 }
    in_class && /^ +@Deprecated/ { dep_m = 1 }
    in_class && /^ +@(GET|POST|PUT|DELETE|PATCH|HEAD)([^A-Za-z]|$)/ { m = $0; gsub(/[ @]/, "", m); sub(/\(.*/, "", m); verb = m }
    in_class && /^ +@Path\("/ { match($0, /"[^"]*"/); s = substr($0, RSTART + 1, RLENGTH - 2); subp = (s ~ /^\// ? s : "/" s) }
    in_class && verb != "" && /^ +(public|protected)? *[A-Za-z<>?, ]+ [a-zA-Z0-9_]+\(/ { flush() }
  ' "$f"
done | grep -E -- "$FILTER" | sort -b -k2,2 -k1,1

cat <<EOF

Contexts: management = Management API (web.http.management.*), public = data plane public API, others = custom ports.
TEST = a controller test exists in the module; DOC = the base path is mentioned in docs/usage (Management API walkthrough).
OpenAPI: generated with './gradlew resolve' into resources/openapi/yaml (published by .github/workflows/publish-openapi-ui.yml).
EOF
