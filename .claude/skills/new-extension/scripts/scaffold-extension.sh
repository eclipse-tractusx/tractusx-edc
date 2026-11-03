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

# Scaffold a Tractus-X EDC extension module with the repository conventions: license headers,
# build.gradle.kts, a ServiceExtension with @Extension/@Setting/@Inject, the META-INF service
# registration, a DependencyInjectionExtension test and a README with a settings table.
# It never edits existing files; it prints the lines to add to settings.gradle.kts and the runtime.
#
# Usage: scaffold-extension.sh <module-dir> <java-package> <ExtensionClassName> [setting.key.prefix]
#   scaffold-extension.sh edc-extensions/cx-foo org.eclipse.tractusx.edc.foo CxFooExtension tx.edc.foo
set -euo pipefail

[ $# -ge 3 ] || { sed -n '/^# Scaffold/,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 1; }
DIR="$1"; PKG="$2"; CLS="$3"; PREFIX="${4:-tx.edc.$(basename "$DIR" | tr '-' '.')}"
ROOT=$(git rev-parse --show-toplevel); cd "$ROOT"
case "$CLS" in *Extension) ;; *) echo "ExtensionClassName must end with 'Extension'" >&2; exit 1 ;; esac
case "$PKG" in org.eclipse.tractusx.edc.*) ;; *) echo "warning: Tractus-X code normally lives under org.eclipse.tractusx.edc.*" >&2 ;; esac
[ ! -e "$DIR" ] || { echo "$DIR already exists; refusing to overwrite" >&2; exit 1; }

YEAR=$(date +%Y)
NAME="${CLS%Extension}"
PKG_DIR=$(echo "$PKG" | tr . /)
GRADLE_PATH=":$(echo "$DIR" | tr / :)"
SETTING_CONST=$(echo "${NAME}" | sed -E 's/([a-z0-9])([A-Z])/\1_\2/g' | tr '[:lower:]' '[:upper:]')_ENABLED

java_header() { cat <<EOF
/********************************************************************************
 * Copyright (c) $YEAR Contributors to the Eclipse Foundation
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
EOF
}
hash_header() { cat <<EOF
#################################################################################
#  Copyright (c) $YEAR Contributors to the Eclipse Foundation
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
EOF
}

mkdir -p "$DIR/src/main/java/$PKG_DIR" "$DIR/src/test/java/$PKG_DIR" "$DIR/src/main/resources/META-INF/services"

{ java_header; cat <<EOF

plugins {
    \`java-library\`
    \`maven-publish\`
}

dependencies {
    implementation(libs.edc.spi.core)
    implementation(project(":spi:core-spi"))

    testImplementation(libs.edc.junit)
}
EOF
} > "$DIR/build.gradle.kts"

{ java_header; cat <<EOF

package $PKG;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static $PKG.$CLS.NAME;

/**
 * TODO: describe what this extension contributes and which dataspace it belongs to
 * (generic core vs. Catena-X specific, see ADR 2026-01-07 multi-dataspace support).
 */
@Extension(value = NAME)
public class $CLS implements ServiceExtension {

    public static final String NAME = "$(echo "$NAME" | sed -E 's/([a-z0-9])([A-Z])/\1 \2/g') Extension";

    static final String $SETTING_CONST = "$PREFIX.enabled";

    @Setting(key = $SETTING_CONST, description = "Enables the $NAME feature", defaultValue = "false")
    private boolean enabled;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (!enabled) {
            monitor.debug("%s is disabled".formatted(NAME));
            return;
        }
        // TODO: register services, transformers, validators or API controllers here
    }
}
EOF
} > "$DIR/src/main/java/$PKG_DIR/$CLS.java"

{ hash_header; echo; echo "$PKG.$CLS"; } > "$DIR/src/main/resources/META-INF/services/org.eclipse.edc.spi.system.ServiceExtension"

{ java_header; cat <<EOF

package $PKG;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static $PKG.$CLS.$SETTING_CONST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class ${CLS}Test {

    private final Monitor monitor = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Monitor.class, monitor);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of($SETTING_CONST, "true")));
    }

    @Test
    void initialize(ServiceExtensionContext context, $CLS extension) {
        extension.initialize(context);

        assertThat(extension.name()).isEqualTo($CLS.NAME);
    }
}
EOF
} > "$DIR/src/test/java/$PKG_DIR/${CLS}Test.java"

cat > "$DIR/README.md" <<EOF
# $(echo "$NAME" | sed -E 's/([a-z0-9])([A-Z])/\1 \2/g') Extension

TODO: one paragraph on what this extension does, which plane it runs in (control plane / data plane)
and whether it is dataspace-agnostic or Catena-X specific.

## Configuration

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| \`$PREFIX.enabled\` | | \`false\` | Enables the $NAME feature |

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: $YEAR Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
EOF

echo "Created $DIR:"; (cd "$DIR" && find . -type f | sed 's#^\./#  #' | sort)
cat <<EOF

Next steps (not done automatically):
  1. settings.gradle.kts, next to related modules:
       include("$GRADLE_PATH")
  2. Ship it in a runtime, e.g. edc-controlplane/edc-controlplane-base/build.gradle.kts
     (or edc-dataplane/edc-dataplane-base/build.gradle.kts):
       implementation(project("$GRADLE_PATH"))
  3. Replace libs.edc.spi.core with the SPIs you really use (gradle/libs.versions.toml).
  4. Expose new settings via the Helm chart if operators need them (see the setting-lifecycle skill).
  5. ./gradlew $GRADLE_PATH:build   (compiles, runs Checkstyle and the unit test)
EOF
