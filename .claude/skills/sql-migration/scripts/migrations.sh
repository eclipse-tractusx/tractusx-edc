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

# Inspect and create Tractus-X EDC Flyway migrations (edc-extensions/migrations/*).
#
# Usage: migrations.sh <command> [args]
#   status                          every migration folder: count, latest version, whether it is released
#   new <folder> "<Description>" [--version V1_10_0]
#                                   create the next migration file in a folder; <folder> is
#                                   'connector' (shared schema, the default for new work), a store folder
#                                   name such as 'asset' or 'edr-index', or a path
#   next <folder>                   print the next version for a folder without creating anything
set -euo pipefail

ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
BASE=edc-extensions/migrations
die() { echo "migrations: $*" >&2; exit 1; }
REL=$(git tag --list '[0-9]*.[0-9]*.[0-9]*' | grep -v -e '-' | sort -t. -k1,1n -k2,2n -k3,3n | tail -1 || true)

vkey() { echo "$1" | sed -E 's/^V//; s/__.*//' | awk -F_ '{printf "%06d%06d%06d%06d\n", $1, $2, $3, $4}'; }
folders() { git ls-files "$BASE/*/src/main/resources/*V*__*.sql" | xargs -n1 dirname | sort -u; }
latest_in() { # newest migration file name in a folder
  for f in $(git ls-files "$1/V*__*.sql" | xargs -n1 basename); do echo "$(vkey "$f") $f"; done | sort | tail -1 | cut -d' ' -f2
}
resolve_folder() {
  case "$1" in
    connector) echo "$BASE/connector-migration/src/main/resources/migrations/connector"; return ;;
    */*) [ -d "$1" ] && { echo "${1%/}"; return; } ;;
  esac
  local m; m=$(folders | grep -E "/$1\$" || true)
  [ -n "$m" ] || die "no migration folder named '$1' (see: migrations.sh status)"
  [ "$(echo "$m" | wc -l | tr -d ' ')" = 1 ] || die "'$1' is ambiguous: $(echo "$m" | tr '\n' ' ')"
  echo "$m"
}
next_version() { # V<a>_<b>_<c>: bump the minor part when the folder uses minors (V1_9_0 -> V1_10_0), else the patch
  local l; l=$(latest_in "$1")
  [ -n "$l" ] || { echo "V0_0_1"; return; }
  echo "${l%%__*}" | sed 's/^V//' | awk -F_ '{ if ($3 == 0 && $2 > 0) printf "V%d_%d_0\n", $1, $2 + 1; else printf "V%d_%d_%d\n", $1, $2, $3 + 1 }'
}
released() { [ -n "$REL" ] && git ls-tree -r --name-only "$REL" -- "$BASE" | grep -F -e "/$1" >/dev/null; }

cmd="${1:-}"; shift || true
case "$cmd" in
  status)
    printf 'Latest release tag: %s\n\n' "${REL:-none}"
    printf '%-45s %5s  %-62s %s\n' "MODULE: FOLDER" "COUNT" "LATEST" "IN ${REL:-release}?"
    for d in $(folders); do
      l=$(latest_in "$d"); n=$(git ls-files "$d/V*__*.sql" | wc -l | tr -d ' ')
      r="no (unreleased)"; released "$l" && r="yes"
      dups=$(git ls-files "$d/V*__*.sql" | xargs -n1 basename | sed -E 's/__.*//' | sort | uniq -d | tr '\n' ' ')
      short=$(echo "${d#"$BASE"/}" | sed -E 's#/src/main/resources/(org/eclipse/tractusx/edc/postgresql/migration|migrations)/#: #')
      printf '%-45s %5s  %-62s %s%s\n' "$short" "$n" "$l" "$r" "${dups:+  DUPLICATE VERSIONS: $dups}"
    done ;;
  next)
    [ $# -ge 1 ] || die "usage: next <folder>"
    d=$(resolve_folder "$1"); echo "$(next_version "$d")  ($d, latest: $(latest_in "$d"))" ;;
  new)
    [ $# -ge 2 ] || die 'usage: new <folder> "<Description>" [--version V1_10_0]'
    d=$(resolve_folder "$1"); desc="$2"; shift 2; ver=""
    [ "${1:-}" = "--version" ] && ver="${2:-}"
    [ -n "$ver" ] || ver=$(next_version "$d")
    echo "$ver" | grep -E '^V[0-9]+_[0-9]+_[0-9]+$' >/dev/null || die "version must look like V1_10_0"
    lk=$(latest_in "$d")
    if [ -n "$lk" ] && [[ "$(vkey "$ver")" < "$(vkey "$lk")" || "$(vkey "$ver")" == "$(vkey "$lk")" ]]; then die "$ver is not newer than $lk"; fi
    name=$(echo "$desc" | sed -E 's/[^A-Za-z0-9]+/_/g; s/^_+|_+$//g' | awk -F_ '{for (i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2)} 1' OFS=_)
    f="$d/${ver}__${name}.sql"
    [ ! -e "$f" ] || die "$f exists"
    cat > "$f" <<EOF
--
--  Copyright (c) $(date +%Y) Contributors to the Eclipse Foundation
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Contributors to the Eclipse Foundation - initial API and implementation
--
--
-- table: TODO
--

-- $desc
-- Keep statements idempotent so re-runs and partially migrated databases are safe, e.g.:
-- ALTER TABLE edc_transfer_process ADD COLUMN IF NOT EXISTS my_column JSON;
EOF
    echo "Created $f"
    cat <<EOF

Next steps:
  1. Write the DDL/DML (idempotent: IF [NOT] EXISTS, guarded UPDATEs). Compare with the upstream schema:
       .claude/skills/edc-upstream-sources/scripts/edc-src.sh schema <store>
  2. Run the migration tests (needs Docker):
       ./gradlew :$(echo "$d" | sed -E 's#/src/main/.*##' | tr / :):test -DincludeTags="PostgresqlIntegrationTest"
  3. Mention it in the upcoming migration guide (migration-guide-and-adr skill) when operators must act
     (long-running data migration, manual steps, disabling via tx.edc.postgresql.migration.*.enabled).
EOF
    ;;
  ""|-h|--help|help) sed -n '/^# Inspect and create/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//' ;;
  *) die "unknown command '$cmd' (try --help)" ;;
esac
