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

# Create a decision record in docs/development/decision-records/<yyyy-mm-dd>-<slug>/README.md
# with the structure the existing records use (Decision, Rationale, Approach, NOTICE under CC-BY-4.0).
#
# Usage: new-adr.sh "<Title>" [--holder "<copyright holder>"] [--date yyyy-mm-dd]
#   new-adr.sh "Retire refresh tokens on acknowledgement" --holder "Cofinity-X GmbH"
set -euo pipefail

usage() { sed -n '/^# Create a decision record/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; }
case "${1:-}" in ""|-h|--help) usage; [ -n "${1:-}" ]; exit $? ;; -*) echo "the title must come first" >&2; usage; exit 1 ;; esac
TITLE="$1"; shift
HOLDER="Contributors to the Eclipse Foundation"; DATE=$(date +%Y-%m-%d)
while [ $# -gt 0 ]; do
  case "$1" in --holder) HOLDER="$2"; shift 2 ;; --date) DATE="$2"; shift 2 ;; *) echo "unknown option $1" >&2; exit 1 ;; esac
done
ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
SLUG=$(echo "$TITLE" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+|-+$//g')
DIR="docs/development/decision-records/$DATE-$SLUG"
[ ! -e "$DIR" ] || { echo "$DIR already exists" >&2; exit 1; }
similar=$(ls docs/development/decision-records | grep -i -E "$(echo "$SLUG" | tr '-' '\n' | awk 'length > 4' | head -3 | paste -sd'|' -)" || true)
mkdir -p "$DIR"
cat > "$DIR/README.md" <<EOF
# $TITLE

## Decision

TODO: one paragraph stating what will be done, in the present or future tense ("The control plane will …").
Name the affected components and whether the change is visible to operators or other participants.

## Rationale

TODO: why the decision is needed. Problem, constraints (Catena-X standards, DSP/DCP specifications, upstream EDC
behaviour), alternatives considered and why they were rejected.

## Approach

TODO: how it will be implemented: new SPIs or extensions (generic vs \`cx-\`), configuration (settings, Helm values),
API changes, persistence and migrations, backward compatibility and rollout.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: ${DATE%%-*} $HOLDER
- Source URL: [https://github.com/eclipse-tractusx/tractusx-edc](https://github.com/eclipse-tractusx/tractusx-edc)
EOF
echo "Created $DIR/README.md"
[ -n "$similar" ] && { echo "Related existing records (check for overlap or supersession):"; echo "$similar" | sed 's/^/  /'; }
echo "Open a PR with only the decision record first (conventional commit 'docs: …'), so the decision is reviewed before the code."
