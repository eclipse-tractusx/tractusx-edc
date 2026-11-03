#!/usr/bin/env bash
#################################################################################
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
#  SPDX-License-Identifier: Apache-2.0
#################################################################################

# Trace one connector setting through every place it lives: the @Setting declaration
# (Tractus-X or upstream EDC), the env var in the Helm templates, the values.yaml entry,
# extension READMEs, docs, migration guides and tests.
#
# Usage: setting-trace.sh <setting.key | ENV_VAR | .Values.path>
#   setting-trace.sh tx.edc.iam.dcp.bdrs.server.url
#   setting-trace.sh TX_EDC_IAM_DCP_BDRS_SERVER_URL
#   setting-trace.sh .Values.controlplane.bdrs.server.url
set -euo pipefail

[ $# -eq 1 ] || { sed -n '/^# Trace one/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 1; }
ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
IN="$1"; KEY=""; ENV=""; HELM=""

key_to_env() { echo "$1" | tr '[:lower:]' '[:upper:]' | tr '.-' '__'; }
helm_to_env() { # env var whose value (or guarding if-block) references the helm path
  local files; files=$(git ls-files 'charts/*/templates/*')
  [ -n "$files" ] || return 0
  awk -v p="$1" '
    /- name: *"?[A-Z][A-Z0-9_]+"?/ { match($0, /[A-Z][A-Z0-9_]+/); last = substr($0, RSTART, RLENGTH); if (pending) { print last; exit } }
    index($0, p) { if ($0 ~ /value/ && last != "") { print last; exit } else pending = 1 }
    FNR == 1 { last = ""; pending = 0 }' $files
}
yaml_line() { # <file> <dotted.path> -> line number of that key in a YAML file
  awk -v target="$2" '
    /^[[:space:]]*(#|$)/ { next }
    match($0, /^ *[A-Za-z0-9_.-]+:/) {
      ind = match($0, /[^ ]/) - 1
      k = $0; sub(/^ */, "", k); sub(/:.*/, "", k)
      while (n > 0 && lvl[n] >= ind) n--
      n++; lvl[n] = ind; key[n] = k
      p = key[1]; for (i = 2; i <= n; i++) p = p "." key[i]
      if (p == target) { print NR; exit }
    }' "$1"
}
hr() { printf '\n== %s\n' "$1"; }
G() { git grep "$@" 2>/dev/null || true; }

case "$IN" in
  .Values.*|Values.*) HELM="${IN#.}"; HELM="${HELM#Values.}"; ENV=$(helm_to_env ".Values.$HELM") ;;
  *[a-z]*) KEY="$IN"; ENV=$(key_to_env "$IN") ;;
  *) ENV="$IN" ;;
esac
if [ -z "$KEY" ] && [ -n "$ENV" ]; then
  # derive the key from the env var by finding a matching dotted string in Java code
  re=$(echo "$ENV" | tr '[:upper:]' '[:lower:]' | sed 's/_/[._-]/g')
  KEY=$(G -hoiE -- "\"$re\"" -- '*.java' | head -1 | tr -d '"')
  if [ -z "$KEY" ]; then
    SRC="$ROOT/.claude/skills/edc-upstream-sources/scripts/edc-src.sh"
    KEY=$(bash "$SRC" settings "^$re\$" 2>/dev/null | head -1 || true)
  fi
fi
printf 'Setting : %s\nEnv var : %s\nHelm    : %s\n' "${KEY:-?}" "${ENV:-?}" "${HELM:-(searching)}"

HAS_CODE=0; HAS_UP=0; HAS_ENV=0; HAS_VALUES=0; HAS_README=0; HAS_MIG=0

hr "Declaration in Tractus-X code"
if [ -n "$KEY" ]; then
  lit=$(G -nF -- "\"$KEY\"" -- ':(glob)**/src/main/**/*.java')
  if [ -n "$lit" ]; then
    HAS_CODE=1; echo "$lit"
    for c in $(echo "$lit" | grep -oE '[A-Z][A-Z0-9_]+ *= *"' | grep -oE '^[A-Z][A-Z0-9_]+' | sort -u); do
      G -nE -- "key *= *([A-Za-z]+\.)?$c([^A-Za-z0-9_]|\$)" -- ':(glob)**/src/main/**/*.java' | sed 's/^/   @Setting -> /'
    done
  else echo "   (not declared in Tractus-X main code)"; fi
fi

hr "Declaration in upstream EDC $(bash .claude/skills/edc-upstream-sources/scripts/edc-src.sh version 2>/dev/null || echo '?')"
if [ -n "$KEY" ]; then
  up=$(bash .claude/skills/edc-upstream-sources/scripts/edc-src.sh grep "\"${KEY//./\\.}\"" 2>/dev/null | head -5 || true)
  if [ -n "$up" ]; then HAS_UP=1; echo "$up"; else echo "   (not an upstream setting)"; fi
fi

hr "Helm templates (env var)"
if [ -n "$ENV" ]; then
  t=$(G -n -A2 -E -- "name: *\"?$ENV\"?\$" -- 'charts/*/templates/*')
  if [ -n "$t" ]; then
    HAS_ENV=1; echo "$t"
    [ -n "$HELM" ] || HELM=$(G -h -A1 -E -- "name: *\"?$ENV\"?\$" -- 'charts/*/templates/*' | grep -oE '\.Values\.[A-Za-z0-9_.]+' | sed 's/^\.Values\.//' | sort -u | tr '\n' ' ')
  else echo "   ($ENV is not set by any chart template; operators would need customEnv/envValueFrom)"; fi
fi

hr "values.yaml"
if [ -n "$HELM" ]; then
  for hp in $HELM; do
    for v in charts/*/values.yaml; do
      ln=$(yaml_line "$v" "$hp")
      if [ -n "$ln" ]; then HAS_VALUES=1; echo "$v:$ln  ($hp)"; sed -n "$((ln > 2 ? ln - 2 : 1)),$((ln))p" "$v" | sed 's/^/   /'; fi
    done
  done
  [ "$HAS_VALUES" = 1 ] || echo "   (.Values.$HELM not found in any values.yaml)"
else echo "   (no Helm value maps to this setting)"; fi

hr "Docs (READMEs, usage docs)"
d=$(G -n -F -e "${KEY:-@@none@@}" -e "${ENV:-@@none@@}" -- '*.md' ':!docs/migration' ':!CHANGELOG.md' ':!.claude' ':!.github')
if [ -n "$d" ]; then echo "$d" | head -15; echo "$d" | grep -q 'README.md' && HAS_README=1; else echo "   (not documented)"; fi

hr "Migration guides (history of renames/removals)"
m=$(G -n -F -e "${KEY:-@@none@@}" -e "${ENV:-@@none@@}" -- docs/migration)
if [ -n "$m" ]; then HAS_MIG=1; echo "$m" | head -10; else echo "   (never mentioned)"; fi

hr "Tests referencing it"
G -lF -e "${KEY:-@@none@@}" -e "${ENV:-@@none@@}" -- ':(glob)**/src/test/**' 'edc-tests' 'charts/*/templates/tests' | sed 's/^/   /' | head -15

ok() { [ "$1" = 1 ] && echo "[x]" || echo "[ ]"; }
hr "Touchpoint summary"
echo "$(ok $HAS_CODE) declared in Tractus-X (@Setting)        $(ok $HAS_UP) declared upstream"
echo "$(ok $HAS_ENV) env var set by a Helm template         $(ok $HAS_VALUES) value in values.yaml"
echo "$(ok $HAS_README) documented in a README                $(ok $HAS_MIG) mentioned in a migration guide"
