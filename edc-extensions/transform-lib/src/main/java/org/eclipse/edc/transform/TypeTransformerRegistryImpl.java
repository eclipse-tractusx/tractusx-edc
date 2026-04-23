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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

public class TypeTransformerRegistryImpl implements TypeTransformerRegistry {
    private final Map<String, Class<?>> aliases = new HashMap<>();
    private final List<TypeTransformer<?, ?>> transformers = new ArrayList<>();
    private final Map<String, TypeTransformerRegistry> contextRegistries = new HashMap<>();
    private TypeTransformerRegistry parent;

    public TypeTransformerRegistryImpl() {
    }

    private TypeTransformerRegistryImpl(TypeTransformerRegistry parent) {
        this.parent = parent;
    }

    @Override
    public void register(TypeTransformer<?, ?> transformer) {
        this.transformers.add(transformer);
    }

    @Override
    public @NotNull TypeTransformerRegistry forContext(String context) {
        return contextRegistries.computeIfAbsent(context, k -> new TypeTransformerRegistryImpl(this));
    }

    @Override
    public @NotNull <INPUT, OUTPUT> TypeTransformer<INPUT, OUTPUT> transformerFor(@NotNull INPUT input, @NotNull Class<OUTPUT> outputType) {
        return transformers.stream()
                .filter(t -> t.getInputType().isInstance(input) && t.getOutputType().equals(outputType))
                .findAny()
                .map(it -> (TypeTransformer<INPUT, OUTPUT>) it)
                .or(() -> Optional.ofNullable(parent).map(p -> p.transformerFor(input, outputType)))
                .orElseThrow(() -> new EdcException(format("No Transformer registered that can handle %s -> %s", input.getClass(), outputType)));
    }

    @Override
    public <INPUT, OUTPUT> Result<OUTPUT> transform(@NotNull INPUT input, @NotNull Class<OUTPUT> outputType) {
        Objects.requireNonNull(input);

        var context = new TransformerContextImpl(this);

        var result = context.transform(input, outputType);
        if (context.hasProblems()) {
            return Result.failure(context.getProblems());
        } else {
            return Result.success(result);
        }
    }

}
