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

# Impact analysis for an upstream EDC version bump: which org.eclipse.edc types that Tractus-X
# code imports were removed, moved or changed between two EDC versions, and which upstream
# @Setting keys disappeared or appeared.
#
# Usage: bump-impact.sh <old-version> <new-version> [--main-only] [--diff]
#   --main-only   only consider imports from src/main (ignore tests)
#   --diff        print the unified diff of every changed type after the summary
set -euo pipefail

[ $# -ge 2 ] || { sed -n '/^# Impact analysis/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 1; }
OLD="$1"; NEW="$2"; shift 2
MAIN_ONLY=0; SHOW_DIFF=0
for a in "$@"; do case "$a" in --main-only) MAIN_ONLY=1 ;; --diff) SHOW_DIFF=1 ;; esac; done

ROOT=$(git rev-parse --show-toplevel)
SRC="$ROOT/.claude/skills/edc-upstream-sources/scripts/edc-src.sh"
[ -x "$SRC" ] || SRC="bash $SRC"
D_OLD=$($SRC path "$OLD"); D_NEW=$($SRC path "$NEW")
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

# 1. Upstream top-level types imported by Tractus-X code, with the number of importing files.
cd "$ROOT"
if [ "$MAIN_ONLY" = 1 ]; then PATHSPEC=':(glob)**/src/main/**/*.java'; else PATHSPEC='*.java'; fi
# count distinct importing files per type (a file with several static imports of one type counts once)
git grep -E '^import (static )?org\.eclipse\.edc\.' -- "$PATHSPEC" \
  | sed -E 's/:import (static )?/ /; s/;.*$//' \
  | awk '{ n = split($2, p, "."); out = p[1]; for (i = 2; i <= n; i++) { out = out "." p[i]; if (p[i] ~ /^[A-Z]/) break } print $1, out }' \
  | grep -vE '\.\*$' | sort -u | awk '{print $2}' | sort | uniq -c | sort -rn > "$TMP/imports"

# file indexes (artifact/path) for fast lookups
(cd "$D_OLD" && find . -name '*.java' | sed 's#^\./##') > "$TMP/idx_old"
(cd "$D_NEW" && find . -name '*.java' | sed 's#^\./##') > "$TMP/idx_new"
locate() { # <old|new> <fqn> -> artifact/path or empty
  local rel; rel="/$(echo "$2" | tr . /).java"
  grep -F -- "$rel" "$TMP/idx_$1" | grep -E -- "^[^/]+$rel\$" | head -1 || true
}

printf "Upstream EDC bump impact: %s -> %s\n" "$OLD" "$NEW"
printf "Tractus-X imports %s distinct upstream types.\n\n" "$(wc -l < "$TMP/imports" | tr -d ' ')"

: > "$TMP/removed"; : > "$TMP/moved"; : > "$TMP/changed"; : > "$TMP/unresolved"; : > "$TMP/pairs"; : > "$TMP/local"
while read -r cnt fqn; do
  fo=$(locate old "$fqn"); fn=$(locate new "$fqn")
  if [ -z "$fo" ] && [ -z "$fn" ]; then
    rel="$(echo "$fqn" | tr . /).java"
    if [ -n "$(git ls-files "*/$rel" | head -1)" ]; then echo "$cnt $fqn" >> "$TMP/local"; else echo "$cnt $fqn" >> "$TMP/unresolved"; fi
    continue
  fi
  if [ -n "$fo" ] && [ -z "$fn" ]; then
    simple="${fqn##*.}"
    cand=$(grep -E -- "/$simple\.java\$" "$TMP/idx_new" | head -3 | tr '\n' ' ' || true)
    if [ -n "$cand" ]; then echo "$cnt $fqn -> $cand" >> "$TMP/moved"; else echo "$cnt $fqn" >> "$TMP/removed"; fi
    continue
  fi
  [ -z "$fo" ] && continue  # new type, already used (e.g. prepared code)
  if ! cmp -s "$D_OLD/$fo" "$D_NEW/$fn"; then
    stat=$(diff "$D_OLD/$fo" "$D_NEW/$fn" | grep -cE '^[<>]' || true)
    echo "$cnt $fqn ($stat changed lines)" >> "$TMP/changed"
    echo "$fo|$fn" >> "$TMP/pairs"
  fi
done < "$TMP/imports"

section() { # <title> <file> <hint>
  local n; n=$(wc -l < "$2" | tr -d ' ')
  printf "## %s: %s\n" "$1" "$n"
  [ "$n" -gt 0 ] && { [ -n "$3" ] && printf "   %s\n" "$3"; sort -rn "$2" | awk '{printf "   %4s files  %s\n", $1, substr($0, index($0,$2))}'; }
  echo
}
section "REMOVED (compile breaks)" "$TMP/removed" "No class with this name exists anymore; find the replacement in the release notes or the diff of its callers."
section "MOVED (fix imports)" "$TMP/moved" "Same simple name found elsewhere in $NEW."
section "CHANGED (review signatures/behaviour)" "$TMP/changed" ""
section "NOT FOUND upstream (other groups like org.eclipse.edc.aws, or wrong FQN)" "$TMP/unresolved" ""
printf "(%s org.eclipse.edc.* types are defined in this repo, e.g. the DSP 0.8 backports, and were skipped.)\n\n" "$(wc -l < "$TMP/local" | tr -d ' ')"

# 2. Upstream settings delta.
$SRC settings . "$OLD" > "$TMP/s_old"; $SRC settings . "$NEW" > "$TMP/s_new"
printf "## Upstream settings removed in %s\n" "$NEW"; comm -23 "$TMP/s_old" "$TMP/s_new" | sed 's/^/   - /'; echo
printf "## Upstream settings added in %s\n" "$NEW"; comm -13 "$TMP/s_old" "$TMP/s_new" | sed 's/^/   + /'; echo
printf "Settings removed upstream that Tractus-X still references:\n"
comm -23 "$TMP/s_old" "$TMP/s_new" | while read -r k; do
  hits=$(git grep -lF -- "$k" -- charts docs ':!docs/migration' ':!docs/release-notes' edc-controlplane edc-dataplane edc-extensions core spi edc-tests 2>/dev/null | head -5 | tr '\n' ' ' || true)
  if [ -n "$hits" ]; then printf "   ! %s  <- %s\n" "$k" "$hits"; fi
done || true
echo
# 3. Upstream SQL schema changes -> Tractus-X Flyway migrations (edc-extensions/migrations/*) need new V…__ files.
printf "## Upstream SQL schema changes (need new Tractus-X Flyway migrations)\n"
S_OLD=$($SRC schema . "$OLD" 2>/dev/null || true); S_NEW=$($SRC schema . "$NEW" 2>/dev/null || true)
any=0
for sf in $(printf '%s\n%s\n' "$S_OLD" "$S_NEW" | sort -u); do
  o=$($SRC schema "^${sf}\$" "$OLD" 2>/dev/null || true); n=$($SRC schema "^${sf}\$" "$NEW" 2>/dev/null || true)
  d=$(diff -wB <(echo "$o") <(echo "$n") | grep -E '^[<>] ' | grep -vE '^[<>] *--' || true)
  if [ -n "$d" ]; then any=1; printf "   %s\n" "$sf"; echo "$d" | sed 's/^/      /' | head -12; fi
done
[ "$any" = 1 ] || echo "   (none)"
echo

echo "Release notes: https://github.com/eclipse-edc/Connector/releases/tag/v$NEW"
echo "Compare:       https://github.com/eclipse-edc/Connector/compare/v$OLD...v$NEW"

if [ "$SHOW_DIFF" = 1 ] && [ -s "$TMP/pairs" ]; then
  echo; echo "################ DIFFS ################"
  while IFS='|' read -r fo fn; do
    diff -u --label "$OLD/$fo" --label "$NEW/$fn" "$D_OLD/$fo" "$D_NEW/$fn" || true
  done < "$TMP/pairs"
fi
