/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.edc.tests.data;

import java.util.List;
import java.util.Objects;


public class Permission {
    private final String action;
    private final List<Constraint> constraints;
    private final String target;


    public Permission(String action, List<Constraint> constraints, String target) {
        this.action = Objects.requireNonNull(action);
        this.constraints = Objects.requireNonNull(constraints);
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public String getTarget() {
        return target;
    }
}
