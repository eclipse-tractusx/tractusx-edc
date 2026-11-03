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

# Reproduce the CI license check locally (.github/actions/generate-and-check-dependencies):
# build the runtime dependency list of the published control plane and data plane exactly like CI,
# compare it with the DEPENDENCIES file CI publishes for main, and optionally run Eclipse Dash on it
# to find restricted or rejected licenses BEFORE the PR is merged.
# DEPENDENCIES in the repository is not edited by hand: CI regenerates and publishes it from main.
#
# Usage: dash-check.sh [--list-only] [--out <dir>] [--dash-version 1.1.0]
#   --list-only   only build the dependency list and diff it against the published file (no Dash run)
#   --out         working directory for the outputs (default: a new temp dir, printed at the end)
set -euo pipefail

LIST_ONLY=0; OUT=""; DASH_VERSION="1.1.0"
while [ $# -gt 0 ]; do
  case "$1" in --list-only) LIST_ONLY=1; shift ;; --out) OUT="$2"; shift 2 ;; --dash-version) DASH_VERSION="$2"; shift 2 ;;
    -h|--help) sed -n '/^# Reproduce the CI license check/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown option $1" >&2; exit 1 ;; esac
done
ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
OUT=${OUT:-$(mktemp -d "${TMPDIR:-/tmp}/tx-dash.XXXXXX")}; mkdir -p "$OUT"
PUBLISHED_URL="https://eclipse-tractusx.github.io/tractusx-edc/DEPENDENCIES"
DASH_JAR="${XDG_CACHE_HOME:-$HOME/.cache}/eclipse-dash/org.eclipse.dash.licenses-$DASH_VERSION.jar"

# 1. Dependency list, same commands and extraction as CI (perl replaces CI's grep -P for macOS).
echo "Resolving runtime dependencies of :edc-controlplane and :edc-dataplane ..." >&2
for p in edc-controlplane edc-dataplane; do
  ./gradlew -q ":$p:allDependencies" --configuration runtimeClasspath \
    | perl -nle 'print $& while /(?<=\s)[\w.-]+:[\w.-]+:[^:\s\[\]]+/g' > "$OUT/dependency-list-$p"
done
cat "$OUT"/dependency-list-edc-* | sort -u > "$OUT/dependency-list"
echo "$(wc -l < "$OUT/dependency-list" | tr -d ' ') distinct coordinates -> $OUT/dependency-list" >&2

# 2. Diff against the DEPENDENCIES published from main (maven/mavencentral/<g>/<a>/<v> -> g:a:v).
if curl -fsSL "$PUBLISHED_URL" -o "$OUT/DEPENDENCIES.published" 2>/dev/null; then
  sed -nE 's#^maven/mavencentral/([^/]+)/([^/]+)/([^,]+),.*#\1:\2:\3#p' "$OUT/DEPENDENCIES.published" | sort -u > "$OUT/published-list"
  comm -23 "$OUT/dependency-list" "$OUT/published-list" > "$OUT/new"
  comm -13 "$OUT/dependency-list" "$OUT/published-list" > "$OUT/gone"
  printf '\n## Not yet in the published DEPENDENCIES (new artifacts or versions): %s\n' "$(wc -l < "$OUT/new" | tr -d ' ')"
  sed 's/^/   + /' "$OUT/new" | head -60
  printf '\n## Published but no longer used: %s\n' "$(wc -l < "$OUT/gone" | tr -d ' ')"
  sed 's/^/   - /' "$OUT/gone" | head -30
  printf '\n## Already restricted in the published file (pending IP review): %s\n' "$(grep -c ', restricted,' "$OUT/DEPENDENCIES.published" || true)"
  grep ', restricted,' "$OUT/DEPENDENCIES.published" | sed 's/^/   ! /' || true
else
  echo "(could not download $PUBLISHED_URL; skipping the diff)" >&2
fi

# 3. Eclipse Dash on the full list (contacts the Eclipse Foundation and ClearlyDefined APIs).
if [ "$LIST_ONLY" = 0 ]; then
  command -v java >/dev/null || { echo "java is required to run Eclipse Dash" >&2; exit 1; }
  if [ ! -s "$DASH_JAR" ]; then
    mkdir -p "$(dirname "$DASH_JAR")"
    curl -fsSL -o "$DASH_JAR" "https://repo.eclipse.org/service/rest/v1/search/assets/download?repository=dash-maven2-releases&maven.groupId=org.eclipse.dash&maven.artifactId=org.eclipse.dash.licenses&maven.baseVersion=$DASH_VERSION&maven.extension=jar"
  fi
  echo; echo "Running Eclipse Dash $DASH_VERSION ..." >&2
  java -jar "$DASH_JAR" "$OUT/dependency-list" -summary "$OUT/DEPENDENCIES" > "$OUT/dash.log" 2>&1 || true
  printf '\n## Eclipse Dash result: %s approved, %s restricted, %s rejected\n' \
    "$(grep -c ', approved,' "$OUT/DEPENDENCIES" || true)" "$(grep -c ', restricted,' "$OUT/DEPENDENCIES" || true)" "$(grep -c ', rejected,' "$OUT/DEPENDENCIES" || true)"
  grep -E ', (restricted|rejected),' "$OUT/DEPENDENCIES" | sed 's/^/   ! /' || true
fi
echo; echo "Outputs in $OUT (DEPENDENCIES in the repository is regenerated by CI on main; do not commit it by hand)."
