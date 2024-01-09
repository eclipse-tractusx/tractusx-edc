/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iatp.ih;

import org.eclipse.edc.identityhub.spi.ScopeToCriterionTransformer;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.store.model.VerifiableCredentialResource;
import org.eclipse.edc.identitytrust.TrustedIssuerRegistry;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiableCredentialContainer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Extension("Identity Hub extension for testing")
public class IdentityHubExtension implements ServiceExtension {

    public static final String DATASPACE_ISSUER = "did:example:dataspace_issuer";

    @Inject
    private CredentialStore credentialStore;

    @Inject
    private TrustedIssuerRegistry registry;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var did = context.getConfig().getString("edc.iam.issuer.id");

        var credential = VerifiableCredential.Builder.newInstance()
                .type("MembershipCredential")
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .claim("holderIdentifier", context.getParticipantId())
                        .build())
                .issuer(new Issuer(DATASPACE_ISSUER, Map.of()))
                .issuanceDate(Instant.now())
                .build();

        var credentialResource = VerifiableCredentialResource.Builder.newInstance()
                .issuerId(DATASPACE_ISSUER)
                .holderId(did)
                .credential(new VerifiableCredentialContainer(getCredential(context.getParticipantId()), CredentialFormat.JSON_LD, credential))
                .build();

        credentialStore.create(credentialResource);

    }
    
    @Provider
    public ScopeToCriterionTransformer scopeToCriterionTransformer() {
        return new TxScopeToCriterionTransformer();
    }

    private String getCredential(String participantId) {
        try {
            var content = getClass().getClassLoader().getResourceAsStream("credentials/" + participantId.toLowerCase() + "-membership.json").readAllBytes();
            return new String(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
