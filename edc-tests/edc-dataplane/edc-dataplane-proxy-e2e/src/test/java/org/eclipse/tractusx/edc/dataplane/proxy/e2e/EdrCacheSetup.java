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

package org.eclipse.tractusx.edc.dataplane.proxy.e2e;

import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.core.defaults.PersistentCacheEntry;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates test EDR cache entries.
 */
public class EdrCacheSetup {

    public static final String AUTHENTICATION = "authentication";
    public static final String ENDPOINT = "http://test.com";

    private static String generateAuthCode() {
        //noinspection StringBufferReplaceableByString
        return new StringBuilder()
                .append("eyJhbGciOiJSUzI1NiIsInZlcn")
                .append("Npb24iOnRydWV9.")
                .append("eyJpc3MiOiJ0ZXN0LWNvb")
                .append("m5lY3RvciIsInN1YiI6ImNvbnN1bW")
                .append("VyLWNvbm5lY3RvciIsImF1ZCI6InRlc3Q")
                .append("tY29ubmVjdG9yIiwi")
                .append("aWF0IjoxNjgxOTEzN")
                .append("jM2LCJleHAiOjMzNDU5NzQwNzg4LCJjaWQiOiIzMmE2M")
                .append("2E3ZC04MGQ2LTRmMmUtOTBlN")
                .append("i04MGJhZjVmYzJiM2MifQ.QAuotoRxpEqfuzkTcTq2w5Tcyy")
                .append("3Rc3UzUjjvNc_zwgNROGLe-wO")
                .append("9tFET1dJ_I5BttRxkngDS37dS4R6lN5YXaGHgcH2rf_FuVcJUS")
                .append("FqTp_usGAcx6m7pQQwqpNdcYgmq0NJp3xP87EFP")
                .append("HAy4kBxB5bqpmx4J-zrj9U_gerZ2WlRqpu0SdgP0S5v5D1Gm-v")
                .append("YkLqgvsugrAWH3Ti7OjC5UMdj0kDFwro2NpMY8SSNryiVvBEv8hn0KZdhhebIqPd")
                .append("hqbEQZ9d8WKzcgoqQ3DBd4ijzkd3Fz7ADD2gy_Hxn8Hi2LcItuB514TjCxYA")
                .append("ncTNqZC_JSFEyuxwcGFVz3LdSXgw")
                .toString();
    }

    public static List<PersistentCacheEntry> createEntries() {
        var list = new ArrayList<PersistentCacheEntry>();

        var edrEntry = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId("79f13b89-59a6-4278-8c8e-8540849dbab8")
                .agreementId("a62d02a3-eea5-4852-86d4-5482db4dffe8")
                .transferProcessId("5355d524-2616-43df-9096-558afffff659")
                .build();
        var edr = EndpointDataReference.Builder.newInstance()
                .id("c470e649-5454-4e4d-b065-782752e5d759")
                .endpoint(ENDPOINT)
                .authKey(AUTHENTICATION)
                .authCode(generateAuthCode())
                .contractId("test-contract-id")
                .build();
        list.add(new PersistentCacheEntry(edrEntry, edr));

        var edrEntry2 = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId("9260f395-3d94-4b8b-bdaa-941ead596ce5")
                .agreementId("d6f73f25-b0aa-4b62-843f-7cfaba532b5b8")
                .transferProcessId("b2859c0a-1a4f-4d10-a3fd-9652d7b3469a")
                .build();
        var edr2 = EndpointDataReference.Builder.newInstance()
                .id("514a4142-3d2a-4936-97c3-7892961c6a58")
                .endpoint(ENDPOINT)
                .authKey(AUTHENTICATION)
                .authCode(generateAuthCode())
                .contractId("test-contract-id")
                .build();
        list.add(new PersistentCacheEntry(edrEntry2, edr2));

        var edrEntry3 = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId("9260f395-3d94-4b8b-bdaa-941ead596ce5")
                .agreementId("7a23333b-03b5-4547-822b-595a54ad6d38")
                .transferProcessId("7a23333b-03b5-4547-822b-595a54ad6d38")
                .build();
        var edr3 = EndpointDataReference.Builder.newInstance()
                .id("3563c5a1-685d-40e5-a380-0b5761523d2d")
                .endpoint(ENDPOINT)
                .contractId("test-contract-id")
                .authKey(AUTHENTICATION)
                .authCode(generateAuthCode())
                .build();

        list.add(new PersistentCacheEntry(edrEntry3, edr3));


        return list;
    }
}

