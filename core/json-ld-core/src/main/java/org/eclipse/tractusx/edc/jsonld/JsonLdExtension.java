/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDC_CONTEXT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_CONTEXT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_PREFIX;

public class JsonLdExtension implements ServiceExtension {

    public static final String CREDENTIALS_V_1 = "https://www.w3.org/2018/credentials/v1";

    public static final String CREDENTIALS_SUMMARY_V_1 = "https://w3id.org/2023/catenax/credentials/summary/v1";
    public static final String CREDENTIALS_SUMMARY_V_1_FALLBACK = "https://catenax-ng.github.io/product-core-schemas/SummaryVC.json";
    public static final String SECURITY_JWS_V1 = "https://w3id.org/security/suites/jws-2020/v1";
    public static final String SECURITY_ED25519_V1 = "https://w3id.org/security/suites/ed25519-2020/v1";
    private static final String PREFIX = "document" + File.separator;
    private static final Map<String, String> FILES = Map.of(
            CREDENTIALS_V_1, PREFIX + "credential-v1.jsonld",
            CREDENTIALS_SUMMARY_V_1, PREFIX + "summary-vc-context-v1.jsonld",
            CREDENTIALS_SUMMARY_V_1_FALLBACK, PREFIX + "summary-vc-context-v1.jsonld",
            SECURITY_JWS_V1, PREFIX + "security-jws-2020.jsonld",
            SECURITY_ED25519_V1, PREFIX + "security-ed25519-2020.jsonld",
            TX_CONTEXT, PREFIX + "tx-v1.jsonld",
            EDC_CONTEXT, PREFIX + "edc-v1.jsonld");
    @Inject
    private JsonLd jsonLdService;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(TX_PREFIX, TX_NAMESPACE);
        FILES.entrySet().stream().map(this::mapToFile)
                .forEach(result -> result.onSuccess(entry -> jsonLdService.registerCachedDocument(entry.getKey(), entry.getValue()))
                        .onFailure(failure -> monitor.warning("Failed to register cached json-ld document: " + failure.getFailureDetail())));
    }

    private Result<Map.Entry<String, File>> mapToFile(Map.Entry<String, String> fileEntry) {
        return getResourceFile(fileEntry.getValue())
                .map(file1 -> Map.entry(fileEntry.getKey(), file1));
    }

    @NotNull
    private Result<File> getResourceFile(String name) {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                return Result.failure(format("Cannot find resource %s", name));
            }

            var filename = Path.of(name).getFileName().toString();
            var parts = filename.split("\\.");
            var tempFile = Files.createTempFile(parts[0], "." + parts[1]);
            Files.copy(stream, tempFile, REPLACE_EXISTING);
            return Result.success(tempFile.toFile());
        } catch (Exception e) {
            return Result.failure(format("Cannot read resource %s: ", name));
        }
    }

}
