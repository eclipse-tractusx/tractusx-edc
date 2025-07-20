/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.tests.tck.dsp;

import org.testcontainers.containers.output.OutputFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class TckTestReporter implements Consumer<OutputFrame> {

    private final List<String> failures = new ArrayList<>();
    private final Pattern failedRegex = Pattern.compile("FAILED: (\\w+:.*)");

    public TckTestReporter() {
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        var line = outputFrame.getUtf8String();
        var failed = failedRegex.matcher(line);
        if (failed.find()) {
            failures.add(failed.group(1));
        }
    }

    public List<String> failures() {
        return new ArrayList<>(failures);
    }

}
