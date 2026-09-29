#!/usr/bin/env python3
#################################################################################
#  Copyright (c) 2026 Cofinity-X GmbH
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

"""
Fix and patch the poutine SARIF output:
  1. Restore empty URI locations for composite action findings.
  2. Patch artifact lengths (-1 → real file size) so GitHub Advanced Security
     can correctly map findings to source lines.
"""

import json
import os
import re

VERIFIED_OWNERS = {
    "actions", "github", "docker", "gradle", "step-security",
    "aws-actions", "google-github-actions", "azure", "hashicorp",
    "slsa-framework", "sigstore", "ossf",
}

with open("results.sarif") as f:
    sarif = json.load(f)

# ── Fix 1: Restore empty URI locations for composite action findings ──────────
# poutine bug: github_action_from_unverified_creator_used in composite action.yml
# files is emitted with empty artifactLocation.uri. Reconstruct from scanned files.
scanned_files = []
for run in sarif.get("runs", []):
    for artifact in run.get("artifacts", []):
        uri = artifact.get("location", {}).get("uri", "")
        if uri and os.path.isfile(uri):
            scanned_files.append(uri)

unverified_locations = []
for filepath in scanned_files:
    with open(filepath) as f:
        for lineno, line in enumerate(f, 1):
            m = re.search(r"uses:\s*([^\s#]+)", line)
            if m:
                ref = m.group(1)
                owner = ref.split("/")[0] if "/" in ref else ""
                if owner and owner not in VERIFIED_OWNERS and not ref.startswith("."):
                    unverified_locations.append((filepath, lineno, ref))

fixed_idx = 0
for run in sarif.get("runs", []):
    for result in run.get("results", []):
        if result.get("ruleId") != "github_action_from_unverified_creator_used":
            continue
        uri = (
            result.get("locations", [{}])[0]
            .get("physicalLocation", {})
            .get("artifactLocation", {})
            .get("uri", "")
        )
        if uri:
            continue
        if fixed_idx < len(unverified_locations):
            filepath, lineno, ref = unverified_locations[fixed_idx]
            result["locations"] = [{
                "physicalLocation": {
                    "artifactLocation": {"uri": filepath},
                    "region": {"startLine": lineno, "endLine": lineno},
                }
            }]
            print(f"Fixed location -> {filepath}:{lineno} ({ref})")
            fixed_idx += 1

# Drop stale fingerprints after location/region rewrites so GitHub recalculates them.
for run in sarif.get("runs", []):
    for result in run.get("results", []):
        result.pop("fingerprints", None)
        result.pop("partialFingerprints", None)

# ── Fix 2: Patch artifact lengths (-1 → real size) ───────────────────────────
# poutine emits length=-1 which causes GitHub Advanced Security to ignore
# startLine and show every finding at line 1 in the Security tab.
for run in sarif.get("runs", []):
    for artifact in run.get("artifacts", []):
        uri = artifact.get("location", {}).get("uri", "").lstrip("./")
        if uri and os.path.isfile(uri):
            artifact["length"] = os.path.getsize(uri)

with open("results-fixed.sarif", "w") as f:
    json.dump(sarif, f)

print(f"Done. Fixed {fixed_idx} location(s). Written to results-fixed.sarif.")
