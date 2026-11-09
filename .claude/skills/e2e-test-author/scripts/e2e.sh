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

# List, run and scaffold Tractus-X EDC end-to-end tests (edc-tests/e2e/*).
#
# Usage: e2e.sh <command> [args]
#   list                                  suites, their test classes and whether they need Docker
#   run <suite> [TestClassPattern]        run one suite exactly like CI (-DincludeTags=EndToEndTest)
#   new <suite> <ClassName>               scaffold a provider/consumer test (Postgres runtimes, DSP 2025-1)
#   compile <suite>                       compile the suite's tests without running them (no Docker needed)
set -euo pipefail

ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
E2E=edc-tests/e2e
die() { echo "e2e: $*" >&2; exit 1; }
suite_dir() { [ -d "$E2E/$1" ] || die "unknown suite '$1' (see: e2e.sh list)"; echo "$E2E/$1"; }
needs_docker() { git grep -lE 'PostgresExtension|Container|KafkaExtension|S3Mock|Floci|Azurite|Testcontainers' -- "$1" >/dev/null 2>&1; }
gradle_path() { echo ":$(echo "$1" | tr / :)"; }

cmd="${1:-}"; shift || true
case "$cmd" in
  list)
    printf '%-36s %-7s %s\n' SUITE DOCKER "TEST CLASSES (@EndToEndTest)"
    for d in "$E2E"/*/; do
      d=${d%/}; s=$(basename "$d")
      cls=$(git grep -l '@EndToEndTest' -- "$d" 2>/dev/null | xargs -n1 basename 2>/dev/null | sed 's/\.java$//' | tr '\n' ' ' || true)
      dk=no; needs_docker "$d" && dk=yes
      printf '%-36s %-7s %s\n' "$s" "$dk" "${cls:-(none)}"
    done
    echo; echo "Shared fixtures: edc-tests/e2e-fixtures/src/testFixtures/java/org/eclipse/tractusx/edc/tests/" ;;
  run)
    [ $# -ge 1 ] || die "usage: run <suite> [TestClassPattern]"
    d=$(suite_dir "$1")
    if needs_docker "$d" && ! docker info >/dev/null 2>&1; then die "suite '$1' needs Docker (Testcontainers), but the Docker daemon is not reachable"; fi
    set -- ./gradlew -p "$d" test -DincludeTags="EndToEndTest" -PverboseTest=true ${2:+--tests "*$2*"}
    echo "+ $*"; "$@"
    echo "JUnit reports: $d/build/reports/tests/test/index.html" ;;
  compile)
    [ $# -ge 1 ] || die "usage: compile <suite>"
    d=$(suite_dir "$1"); ./gradlew -q "$(gradle_path "$d"):compileTestJava" "$(gradle_path "$d"):checkstyleTest" && echo "compiled + checkstyle OK: $d" ;;
  new)
    [ $# -ge 2 ] || die "usage: new <suite> <ClassName>"
    d=$(suite_dir "$1"); cls="$2"
    case "$cls" in *Test) ;; *) die "class name must end with 'Test'" ;; esac
    ex=$(git ls-files "$d/src/test/java/*.java" | head -1)
    pkg=$( [ -n "$ex" ] && sed -n 's/^package \(.*\);/\1/p' "$ex" | head -1 || true)
    pkg=${pkg:-org.eclipse.tractusx.edc.tests.$(echo "$1" | tr -d '-' )}
    f="$d/src/test/java/$(echo "$pkg" | tr . /)/$cls.java"
    [ ! -e "$f" ] || die "$f exists"
    mkdir -p "$(dirname "$f")"
    cat > "$f" <<EOF
/********************************************************************************
 * Copyright (c) $(date +%Y) Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package $pkg;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.emptyPolicy;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

/**
 * TODO: describe the scenario under test (which feature, which plane, which policy/protocol aspect).
 */
@EndToEndTest
public class $cls {

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_DID)
            .bpn(CONSUMER_BPN)
            .protocol(DSP_2025, DSP_2025_PATH)
            .build();

    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .protocol(DSP_2025, DSP_2025_PATH)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    @BeforeEach
    void setup() {
        CONSUMER.setJsonLd(CONSUMER_RUNTIME.getService(JsonLd.class));
    }

    @Test
    @DisplayName("TODO: describe the expected behaviour")
    void consumerSeesOffer() {
        // arrange: provider publishes an asset with access and contract policies
        PROVIDER.createAsset("e2e-asset");
        var accessPolicyId = PROVIDER.createPolicyDefinition(emptyPolicy());
        var contractPolicyId = PROVIDER.createPolicyDefinition(emptyPolicy());
        PROVIDER.createContractDefinition("e2e-asset", "e2e-definition", accessPolicyId, contractPolicyId);

        // act
        var datasets = CONSUMER.getCatalogDatasets(PROVIDER);

        // assert
        assertThat(datasets).hasSize(1)
                .allSatisfy(dataset -> assertThat(getDatasetAssetId(dataset.asJsonObject())).isEqualTo("e2e-asset"));
    }
}
EOF
    echo "Created $f"
    echo "Compile it: .claude/skills/e2e-test-author/scripts/e2e.sh compile $1   (run needs Docker: e2e.sh run $1 $cls)" ;;
  ""|-h|--help|help) sed -n '/^# List, run and scaffold/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//' ;;
  *) die "unknown command '$cmd' (try --help)" ;;
esac
