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

# Heuristic breaking-change scan of the current change set (committed + uncommitted)
# against a base. It flags what operators, adopters or other dataspace participants would
# notice, and which follow-ups (migration guide, helm-docs, DEPENDENCIES, tests) are missing.
# It is a triage aid: every finding still needs a human (or agent) judgement.
#
# Usage: bc-scan.sh [base-ref] [--strict]
#   base-ref   default: origin/main (compared from the merge base, including the working tree)
#   --strict   exit 1 if any BLOCKER or BREAKING finding exists
set -euo pipefail

BASE="origin/main"; STRICT=0
for a in "$@"; do case "$a" in --strict) STRICT=1 ;; -h|--help) sed -n '/^# Heuristic/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0 ;; *) BASE="$a" ;; esac; done
ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
MB=$(git merge-base "$BASE" HEAD) || { echo "bc-scan: cannot find merge base with $BASE" >&2; exit 2; }
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

# change set: committed since merge base + staged + unstaged + untracked
git diff --name-status "$MB" > "$TMP/status"
git ls-files --others --exclude-standard | sed 's/^/A\t/' >> "$TMP/status"
CHANGED=$(cut -f2- "$TMP/status" | awk -F'\t' '{print $NF}')
[ -n "$CHANGED" ] || { echo "No changes against $BASE ($MB)."; exit 0; }

N_BLOCK=0; N_BREAK=0; N_HIGH=0; N_WARN=0
finding() { # <severity> <message>
  case "$1" in BLOCKER) N_BLOCK=$((N_BLOCK+1)) ;; BREAKING) N_BREAK=$((N_BREAK+1)) ;; HIGH) N_HIGH=$((N_HIGH+1)) ;; WARN) N_WARN=$((N_WARN+1)) ;; esac
  printf '%-8s %s\n' "$1" "$2"
}
changed_match() { echo "$CHANGED" | grep -E -- "$1" || true; }
removed_lines() { git diff -U0 "$MB" -- "$@" | grep -E '^-[^-]' | sed 's/^-//' || true; }
added_lines()   { { git diff -U0 "$MB" -- "$@" | grep -E '^\+[^+]' | sed 's/^+//'; for f in $(git ls-files --others --exclude-standard -- "$@"); do cat "$f"; done; } || true; }
yaml_paths() { # all key paths of a YAML document on stdin
  awk '/^[[:space:]]*(#|$)/ {next}
       match($0, /^ *[A-Za-z0-9_.-]+:/) { ind = match($0, /[^ ]/) - 1; k = $0; sub(/^ */, "", k); sub(/:.*/, "", k)
         while (n > 0 && lvl[n] >= ind) n--; n++; lvl[n] = ind; key[n] = k
         p = key[1]; for (i = 2; i <= n; i++) p = p "." key[i]; print p }' | sort -u
}
vnum() { echo "$1" | sed -E 's/^V//; s/__.*//' | awk -F_ '{printf "%06d%06d%06d%06d\n", $1, $2, $3, $4}'; }

echo "Breaking-change scan: $(git rev-parse --abbrev-ref HEAD) vs $BASE (merge base ${MB:0:9})"
echo "$(echo "$CHANGED" | wc -l | tr -d ' ') files changed"
echo

# newest final release tag (release tags live on the releases branch, so not via git describe)
REL=$(git tag --list '[0-9]*.[0-9]*.[0-9]*' | grep -v -e '-' | sort -t. -k1,1n -k2,2n -k3,3n | tail -1 || true)

# 1. SQL migrations: applied migrations are immutable; new ones must be strictly newer and unique.
while IFS=$'\t' read -r st f rest; do
  f="${rest:-$f}"
  case "$f" in *migrations/*/V*__*.sql) ;; *) continue ;; esac
  case "$f" in */src/test/*) continue ;; esac
  case "$st" in
    M*|D*|R*)
      # match by file name: migration folders have moved between releases
      if [ -n "$REL" ] && git ls-tree -r --name-only "$REL" -- edc-extensions/migrations | grep -qF -e "/$(basename "$f")"; then
        finding BLOCKER "Released Flyway migration modified/removed: $f (shipped in $REL; applied migrations are immutable, add a new V…__ file instead)"
      else
        finding WARN "Unreleased Flyway migration modified: $f (not in ${REL:-any release tag}; fine only if no deployed snapshot ran it)"
      fi ;;
    A*) d=$(dirname "$f"); me=$(basename "$f"); mv=$(vnum "$me")
        existing=$(git ls-tree --name-only "$MB" "$d/" 2>/dev/null | xargs -n1 basename 2>/dev/null | grep -E '^V.*__.*\.sql$' || true)
        dup=$(for o in $existing; do [ "$(vnum "$o")" = "$mv" ] && echo "$o"; done || true)
        newest=$(for o in $existing; do echo "$(vnum "$o") $o"; done | sort | tail -1 | cut -d' ' -f2 || true)
        if [ -n "$dup" ]; then finding BLOCKER "Duplicate Flyway version in $d: $me vs $dup"
        elif [ -n "$newest" ] && [[ "$(vnum "$newest")" > "$mv" ]]; then finding BLOCKER "New migration $me is older than the newest existing $newest in $d (out-of-order; Flyway will refuse or skip it)"; fi
        finding HIGH "New DB migration $f: verify upgradeability (upgradeability-test workflow, PostgresqlIntegrationTest) and mention it in the migration guide" ;;
  esac
done < "$TMP/status"

# 2. Settings removed or renamed (string literals that look like setting keys).
JAVA_MAIN=':(glob)**/src/main/**/*.java'
removed_lines "$JAVA_MAIN" | grep -oE '"(edc|tx|tractusx|web)\.[A-Za-z0-9.<>_-]+"' | tr -d '"' | sort -u > "$TMP/k_rem" || true
added_lines "$JAVA_MAIN" | grep -oE '"(edc|tx|tractusx|web)\.[A-Za-z0-9.<>_-]+"' | tr -d '"' | sort -u > "$TMP/k_add" || true
for k in $(comm -23 "$TMP/k_rem" "$TMP/k_add"); do
  if ! git grep -qF -- "\"$k\"" -- "$JAVA_MAIN"; then
    finding BREAKING "Setting key removed/renamed: $k (deprecate first or add a migration-guide row old -> new + env var)"
  fi
done
NEW_KEYS=$(comm -13 "$TMP/k_rem" "$TMP/k_add" | while read -r k; do git grep -qF -e "\"$k\"" "$MB" -- "$JAVA_MAIN" 2>/dev/null || echo "$k"; done || true)

# 3. Helm values removed; env vars no longer set; helm-docs not regenerated.
for v in $(changed_match '^charts/[^/]+/values\.yaml$'); do
  git show "$MB:$v" 2>/dev/null | yaml_paths > "$TMP/hv_old" || : > "$TMP/hv_old"
  [ -f "$v" ] && yaml_paths < "$v" > "$TMP/hv_new" || : > "$TMP/hv_new"
  # report only the root of each removed subtree
  comm -23 "$TMP/hv_old" "$TMP/hv_new" | awk '{ if (r != "" && index($0, r ".") == 1) next; r = $0; print }' > "$TMP/hv_rem"
  while read -r p; do
    kids=$(grep -c -- "^$p\." "$TMP/hv_old" || true)
    finding BREAKING "Helm value removed/renamed in $v: $p$([ "$kids" -gt 0 ] && echo " (+$kids nested keys)") (operators' values files will silently stop working)"
  done < "$TMP/hv_rem"
  chart=$(dirname "$v")
  if ! echo "$CHANGED" | grep -qx "$chart/README.md"; then finding WARN "$v changed but $chart/README.md not regenerated (run helm-docs; CI job verify-helm-docs fails otherwise)"; fi
done
removed_lines 'charts/*/templates/*' | grep -oE -- '- name: *"?[A-Z][A-Z0-9_]+"?' | grep -oE '[A-Z][A-Z0-9_]+' | sort -u > "$TMP/e_rem" || true
for e in $(cat "$TMP/e_rem"); do
  git grep -qE -- "name: *\"?$e\"?\$" -- 'charts/*/templates/*' || finding BREAKING "Env var no longer set by the Helm chart: $e"
done

# 4. Management/public API paths removed.
removed_lines "$JAVA_MAIN" | grep -oE '@Path\("[^"]+"\)' | sort -u > "$TMP/p_rem" || true
added_lines "$JAVA_MAIN" | grep -oE '@Path\("[^"]+"\)' | sort -u > "$TMP/p_add" || true
for p in $(comm -23 "$TMP/p_rem" "$TMP/p_add"); do
  git grep -qF -- "$p" -- "$JAVA_MAIN" || finding BREAKING "API path removed: $p (keep it deprecated for a release, update OpenAPI in resources/openapi and the walkthrough)"
done
api_mods=$(changed_match '/[a-z0-9-]*api[a-z0-9-]*/src/main/.*\.java$' | sed -E 's#/src/main/.*##' | sort -u)
for m in $api_mods; do
  finding WARN "API module changed: $m (check request/response JSON-LD shape and validators; add/adjust ApiTest)"
done

# 5. JSON-LD contexts, namespaces and vocabulary.
for f in $(changed_match '^core/json-ld-[^/]+/src/main/resources/document/'); do finding HIGH "Cached JSON-LD context changed: $f (affects every participant expanding these terms)"; done
removed_lines "$JAVA_MAIN" | grep -E 'String [A-Z0-9_]*(NS|CONTEXT|PREFIX|NAMESPACE|VOCAB)[A-Z0-9_]* *= *"https?://' | sed -E 's/^ *//' | sort -u > "$TMP/ns_rem" || true
added_lines "$JAVA_MAIN" | grep -E 'String [A-Z0-9_]*(NS|CONTEXT|PREFIX|NAMESPACE|VOCAB)[A-Z0-9_]* *= *"https?://' | sed -E 's/^ *//' | sort -u > "$TMP/ns_add" || true
while read -r l; do finding HIGH "Namespace/context constant removed or changed: $l"; done < <(comm -23 "$TMP/ns_rem" "$TMP/ns_add")

# 6. Protocol surfaces: DSP (ADR 2025-02-12: no breaking changes within a DSP version) and DCP.
dsp=$(changed_match '^(edc-extensions/dsp/|edc-extensions/dataspace-protocol/|spi/dsp-spi-08/).*src/main/')
[ -n "$dsp" ] && finding HIGH "DSP implementation changed ($(echo "$dsp" | wc -l | tr -d ' ') files): verify wire compatibility per ADR 2025-02-12; run -DincludeTags=CompatibilityTest and the dsp-tck-tests"
dcp=$(changed_match '^edc-extensions/(dcp|bdrs-client|did-document)/.*src/main/')
[ -n "$dcp" ] && finding HIGH "Identity/DCP implementation changed ($(echo "$dcp" | wc -l | tr -d ' ') files): run dcp-tests and dcp-tck-tests"

# 7. Public SPI signatures (adopters build extensions against these).
for f in $(changed_match '^spi/.*/src/main/.*\.java$'); do
  sig=$(removed_lines "$f" | grep -E '^\s*(public |protected |default |static |abstract |final |<)*[A-Za-z0-9_<>?,. \[\]]+ [a-zA-Z_][A-Za-z0-9_]*\([^;{]*\)?\s*[;{]?\s*$' | grep -vE '^\s*(//|\*|/\*|return|if|for|while)' || true)
  [ -n "$sig" ] && finding WARN "SPI signature removed/changed in $f: $(echo "$sig" | head -2 | sed -E 's/^ +//' | tr '\n' ' ')"
done

# 8. Runtime composition (features shipped in the images).
for f in $(changed_match '^edc-(controlplane|dataplane)/[^/]+/build\.gradle\.kts$'); do
  while read -r l; do finding WARN "Removed from runtime $f: $l (feature disappears from the published image)"; done < <(removed_lines "$f" | grep -E '(project\(|libs\.)' | sed -E 's/^ +//' || true)
done

# 9. Upstream EDC version bump.
if removed_lines gradle/libs.versions.toml | grep -qE '^edc *='; then
  o=$(removed_lines gradle/libs.versions.toml | grep -E '^edc *=' | cut -d'"' -f2); n=$(added_lines gradle/libs.versions.toml | grep -E '^edc *=' | cut -d'"' -f2)
  finding HIGH "Upstream EDC bumped $o -> $n: run .claude/skills/upstream-edc-bump/scripts/bump-impact.sh $o $n and follow the upstream-edc-bump skill"
fi

# 10. Follow-ups.
echo
if [ $((N_BLOCK + N_BREAK + N_HIGH)) -gt 0 ] && [ -z "$(changed_match '^docs/migration/')" ]; then
  finding WARN "Breaking/high-impact findings but no docs/migration/* change: add the entries to the upcoming migration guide"
fi
if [ -n "$(changed_match '^gradle/libs\.versions\.toml$|build\.gradle\.kts$')" ] && \
   git diff -U0 "$MB" -- gradle/libs.versions.toml '*build.gradle.kts' | grep -E '^\+' | grep -v 'project(' | grep -qE '(libs\.[a-z]|version|= *"[0-9])' && \
   [ -z "$(changed_match '^DEPENDENCIES$')" ]; then
  finding WARN "Dependency declarations changed but DEPENDENCIES not updated: regenerate it if a new third-party artifact or version is introduced (Eclipse Dash; docs/development/release.md)"
fi
for k in $NEW_KEYS; do
  env=$(echo "$k" | tr '[:lower:]' '[:upper:]' | tr '.-' '__')
  git grep -qF -e "$k" -e "$env" -- '*.md' || finding WARN "New setting $k ($env) is not documented in any README"
done

echo
printf 'Summary: %d BLOCKER, %d BREAKING, %d HIGH, %d WARN\n' "$N_BLOCK" "$N_BREAK" "$N_HIGH" "$N_WARN"
[ "$STRICT" = 1 ] && [ $((N_BLOCK + N_BREAK)) -gt 0 ] && exit 1
exit 0
