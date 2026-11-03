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

# Read upstream Eclipse EDC sources at an exact version (default: the version pinned in
# gradle/libs.versions.toml). Sources come from the local Gradle cache; missing ones are
# downloaded from Maven Central. Everything is extracted to a cache outside the repository.
#
# Usage: edc-src.sh <command> [args]
#   version                       print the pinned upstream EDC version
#   path     [version]            extract sources (once) and print the extracted directory
#   find     <regex> [version]    list source files whose path matches <regex> (e.g. 'ContractNegotiationManager')
#   read     <Class|regex> [version]   print the source of the best match (other matches are listed)
#   grep     <regex> [version]    grep all upstream .java sources (extended regex)
#   diff     <Class|regex> <old> <new>  unified diff of a class between two versions
#   settings [regex] [version]    list upstream @Setting keys (optionally filtered)
#   schema   [regex] [version]    list upstream SQL schema files, or print them when exactly one matches
#                                 (compare versions: diff <(edc-src.sh schema asset-index 0.16.0) <(edc-src.sh schema asset-index 0.17.0))
#
# Environment: EDC_SRC_CACHE (default ~/.cache/edc-sources), GRADLE_USER_HOME, EDC_MAVEN_REPO,
#              EDC_SRC_OFFLINE=1 (never download).
set -euo pipefail

ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
GRADLE_EDC="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/org.eclipse.edc"
CACHE="${EDC_SRC_CACHE:-${XDG_CACHE_HOME:-$HOME/.cache}/edc-sources}"
MAVEN="${EDC_MAVEN_REPO:-https://repo1.maven.org/maven2}/org/eclipse/edc"

die() { echo "edc-src: $*" >&2; exit 1; }

pinned_version() {
  local v
  v=$(grep -E '^edc *= *"' "$ROOT/gradle/libs.versions.toml" 2>/dev/null | head -1 | cut -d'"' -f2)
  [ -n "$v" ] || die "cannot read 'edc' version from $ROOT/gradle/libs.versions.toml"
  echo "$v"
}

# Artifact names known for a version: from the Gradle cache, falling back to the pinned version's list.
artifact_list() {
  local v="$1" list=""
  if [ -d "$GRADLE_EDC" ]; then
    list=$(cd "$GRADLE_EDC" && for a in *; do [ -d "$a/$v" ] && echo "$a"; done)
    if [ -z "$list" ]; then
      local p; p=$(pinned_version)
      list=$(cd "$GRADLE_EDC" && for a in *; do [ -d "$a/$p" ] && echo "$a"; done)
    fi
  fi
  [ -n "$list" ] || die "no org.eclipse.edc artifacts in the Gradle cache; run ./gradlew compileJava once"
  echo "$list"
}

# Extract all sources of a version into $CACHE/<version>/<artifact>/ (idempotent).
ensure_extracted() {
  local v="$1" dir="$CACHE/$1"
  [ -f "$dir/.complete" ] && { echo "$dir"; return; }
  mkdir -p "$dir" "$CACHE/jars/$v"
  local a jar missing=0 extracted=0 list
  list=$(artifact_list "$v")
  # download sources that are not in the Gradle cache, in parallel
  if [ "${EDC_SRC_OFFLINE:-0}" != "1" ]; then
    for a in $list; do
      [ -d "$dir/$a" ] && continue
      [ -n "$(find "$GRADLE_EDC/$a/$v" -name "$a-$v-sources.jar" 2>/dev/null | head -1)" ] && continue
      [ -s "$CACHE/jars/$v/$a-$v-sources.jar" ] && continue
      echo "$a"
    done | xargs -n 1 -P 8 sh -c 'f="$1/$4-$2-sources.jar"; curl -fsSL -o "$f.part" "$3/$4/$2/$4-$2-sources.jar" 2>/dev/null && mv "$f.part" "$f" || rm -f "$f.part"' _ "$CACHE/jars/$v" "$v" "$MAVEN" || true
  fi
  rm -f "$dir/.missing"
  for a in $list; do
    [ -d "$dir/$a" ] && { extracted=$((extracted + 1)); continue; }
    jar=$(find "$GRADLE_EDC/$a/$v" -name "$a-$v-sources.jar" 2>/dev/null | head -1 || true)
    [ -n "$jar" ] || jar="$CACHE/jars/$v/$a-$v-sources.jar"
    if [ ! -s "$jar" ]; then echo "$a" >> "$dir/.missing"; missing=$((missing + 1)); continue; fi
    mkdir -p "$dir/$a" && unzip -qo "$jar" -d "$dir/$a" '*.java' 2>/dev/null || true
    extracted=$((extracted + 1))
  done
  [ "$extracted" -gt 0 ] || die "no sources found for EDC $v"
  echo "extracted $extracted artifacts for EDC $v ($missing without sources, e.g. BOMs) -> $dir" >&2
  # only mark complete when (almost) everything is there, so a failed download is retried next time
  if [ $((missing * 10)) -le "$extracted" ]; then touch "$dir/.complete"; else
    echo "edc-src: warning: $missing artifacts missing (offline or not published for $v?); will retry next run" >&2; fi
  echo "$dir"
}

# Extract the SQL schemas (shipped in the main jars of the *-sql store artifacts) into $CACHE/<version>-sql/.
ensure_sql_extracted() {
  local v="$1" dir="$CACHE/$1-sql" a jar
  [ -f "$dir/.complete" ] && { echo "$dir"; return; }
  mkdir -p "$dir" "$CACHE/jars/$v"
  for a in $(artifact_list "$v" | grep -E 'sql' | grep -vE 'bom$'); do
    jar=$(find "$GRADLE_EDC/$a/$v" -name "$a-$v.jar" 2>/dev/null | head -1 || true)
    if [ -z "$jar" ]; then
      jar="$CACHE/jars/$v/$a-$v.jar"
      [ -s "$jar" ] || [ "${EDC_SRC_OFFLINE:-0}" = "1" ] || curl -fsSL -o "$jar" "$MAVEN/$a/$v/$a-$v.jar" 2>/dev/null || rm -f "$jar"
    fi
    [ -s "$jar" ] && { mkdir -p "$dir/$a"; unzip -qo "$jar" -d "$dir/$a" '*.sql' 2>/dev/null || true; }
  done
  find "$dir" -type d -empty -delete 2>/dev/null || true
  touch "$dir/.complete"; echo "$dir"
}

find_files() { # <regex> <dir>
  (cd "$2" && find . -name '*.java' | sed 's#^\./##' | grep -E -- "$1" || true)
}

best_match() { # prefer an exact class-name match
  local m="$1" pat="$2" exact
  exact=$(echo "$m" | grep -E "/${pat}\.java$" | head -1 || true)
  [ -n "$exact" ] && echo "$exact" || echo "$m" | head -1
}

cmd="${1:-}"; shift || true
case "$cmd" in
  version) pinned_version ;;
  path) ensure_extracted "${1:-$(pinned_version)}" ;;
  find)
    [ $# -ge 1 ] || die "usage: find <regex> [version]"
    dir=$(ensure_extracted "${2:-$(pinned_version)}")
    find_files "$1" "$dir" | awk -F/ '{a=$1; sub(/^[^\/]+\//,""); printf "%-40s %s\n", a, $0}' ;;
  read)
    [ $# -ge 1 ] || die "usage: read <Class|regex> [version]"
    v="${2:-$(pinned_version)}"; dir=$(ensure_extracted "$v")
    m=$(find_files "$1" "$dir"); [ -n "$m" ] || die "no source matching '$1' in EDC $v"
    f=$(best_match "$m" "$1")
    n=$(echo "$m" | wc -l | tr -d ' ')
    [ "$n" -gt 1 ] && { echo "# $n matches; showing the best one. Others:" ; echo "$m" | grep -vxF "$f" | sed 's/^/#   /' | head -20; }
    echo "# org.eclipse.edc:${f%%/*}:$v  ->  ${f#*/}"
    cat -n "$dir/$f" ;;
  grep)
    [ $# -ge 1 ] || die "usage: grep <regex> [version]"
    dir=$(ensure_extracted "${2:-$(pinned_version)}")
    (cd "$dir" && grep -rnE --include='*.java' -- "$1" . | sed 's#^\./##' || true) ;;
  diff)
    [ $# -ge 3 ] || die "usage: diff <Class|regex> <old-version> <new-version>"
    d1=$(ensure_extracted "$2"); d2=$(ensure_extracted "$3")
    m1=$(find_files "$1" "$d1"); m2=$(find_files "$1" "$d2")
    f1=""; f2=""
    [ -n "$m1" ] && f1=$(best_match "$m1" "$1")
    [ -n "$m2" ] && f2=$(best_match "$m2" "$1")
    [ -n "$f1" ] || [ -n "$f2" ] || die "'$1' not found in $2 or $3"
    [ -n "$f1" ] || { echo "# added in $3: $f2"; exit 0; }
    [ -n "$f2" ] || { echo "# removed in $3 (was $f1). Same simple name elsewhere in $3:"; find_files "/$(basename "$f1")$" "$d2" | sed 's/^/#   /'; exit 0; }
    [ "${f1#*/}" = "${f2#*/}" ] || echo "# moved: $f1 -> $f2"
    diff -u --label "$2/$f1" --label "$3/$f2" "$d1/$f1" "$d2/$f2" || true ;;
  settings)
    dir=$(ensure_extracted "${2:-$(pinned_version)}")
    # keys written inline in @Setting(...) blocks, plus setting-like String constants (key = CONSTANT style)
    (cd "$dir" && {
      grep -rhE --include='*.java' -A4 '@Setting\(' . | grep -oE '"(edc|web|tx|tractusx)\.[A-Za-z0-9.<>_-]+"' || true
      grep -rhE --include='*.java' 'static final String [A-Z0-9_]+ *= *"(edc|web)\.[a-z0-9.<>_-]+"' . | grep -oE '"[^"]+"' || true
    } | tr -d '"' | sort -u | grep -E -- "${1:-.}" || true) ;;
  schema)
    dir=$(ensure_sql_extracted "${2:-$(pinned_version)}")
    m=$(cd "$dir" && find . -name '*.sql' | sed 's#^\./##' | grep -E -- "${1:-.}" | sort || true)
    [ -n "$m" ] || die "no SQL schema matching '${1:-.}'"
    if [ "$(echo "$m" | wc -l | tr -d ' ')" = 1 ]; then cat "$dir/$m"; else echo "$m"; fi ;;
  ""|-h|--help|help) sed -n '/^# Read upstream/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//' ;;
  *) die "unknown command '$cmd' (try --help)" ;;
esac
