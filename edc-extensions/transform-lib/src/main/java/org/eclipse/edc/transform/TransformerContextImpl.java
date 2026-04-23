/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.transform;

import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class TransformerContextImpl implements TransformerContext {
    private final List<String> problems = new ArrayList<>();
    private final TypeTransformerRegistry registry;
    private final Map<Class<?>, Map<String, AtomicReference<?>>> data = new HashMap<>();

    public TransformerContextImpl(TypeTransformerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    @Override
    public List<String> getProblems() {
        return problems;
    }

    @Override
    public void reportProblem(String problem) {
        problems.add(problem);
    }

    @Override
    public ProblemBuilder problem() {
        return new ProblemBuilder(this);
    }

    @Override
    public <INPUT, OUTPUT> @Nullable OUTPUT transform(INPUT object, Class<OUTPUT> outputType) {
        if (object == null) {
            return null;
        }

        return registry.transformerFor(object, outputType)
                .transform(object, this);
    }

    @Override
    public void setData(Class<?> type, String key, Object value) {
        data.computeIfAbsent(type, t -> new HashMap<>()).put(key, new AtomicReference<>(value));
    }

    @Override
    public Object consumeData(Class<?> type, String key) {
        return Optional.of(type)
                .map(data::get)
                .map(typeMap -> typeMap.get(key))
                .map(reference -> reference.getAndSet(null))
                .orElse(null);
    }

}
