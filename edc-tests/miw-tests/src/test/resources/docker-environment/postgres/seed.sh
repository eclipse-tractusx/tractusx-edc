#!/bin/bash
#
# Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0
#
# SPDX-License-Identifier: Apache-2.0
#
# Contributors:
#       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
#
#

set -e

psql -v ON_ERROR_STOP=1 --username "keycloak" --dbname "keycloak" <<-EOSQL
    -- the following lines add one additional participant to MiW, used for the impersonation attack test
    \c miw
    INSERT INTO public.wallet (id, name, did, bpn, algorithm, did_document, created_at, modified_at, modified_from) VALUES (2, 'Another Participant', 'did:web:localhost%3A8000:BPNL000000000042', 'BPNL000000000042', 'ED25519', '{"verificationMethod":[{"publicKeyJwk":{"kty":"OKP","crv":"Ed25519","x":"Xok4qFXhNjMC3l-VHoQBJ_RHhtDmxevaoN13PE3j8MY"},"controller":"did:web:localhost%3A8000:BPNL000000000000","id":"did:web:localhost%3A8000:BPNL000000000000#","type":"JsonWebKey2020"}],"@context":"https://www.w3.org/ns/did/v1","id":"did:web:localhost%3A8000:BPNL000000000000"}', '2023-06-29 13:49:07.138000', '2023-06-29 13:49:07.140000', null);
    INSERT INTO public.issuers_credential (id, holder_did, issuer_did, credential_id, data, type, created_at, modified_at, modified_from) VALUES (3, 'did:web:localhost%3A8000:BPNL000000000042', 'did:web:localhost%3A8000:BPNL000000000042', 'a043c406-d51d-4672-ad89-517c68d025f9', '{"issuanceDate":"2023-06-29T13:49:11Z","credentialSubject":[{"holderIdentifier":"BPNL000000000000","id":"did:web:localhost%3A8000:BPNL000000000000","type":"SummaryCredential","items":["BpnCredential"],"contractTemplates":"https://public.catena-x.org/contracts/"}],"id":"a043c406-d51d-4672-ad89-517c68d025f9","proof":{"assertionMethod":"did:web:localhost%3A8000:BPNL000000000000#","proofPurpose":"proofPurpose","type":"JsonWebSignature2020","created":"2023-06-29T13:49:11Z","jws":"eyJhbGciOiJFZERTQSJ9..waFlAQyE42TddNz0v4q_MkIbSgbjexDQqdt-k6LEQG3BvhE1Adj6SIERtUzmYowuLtdcTBbwVAROf9yzpfNMAg"},"type":["VerifiableCredential","SummaryCredential"],"@context":["https://www.w3.org/2018/credentials/v1","https://catenax-ng.github.io/product-core-schemas/SummaryVC.json"],"issuer":"did:web:localhost%3A8000:BPNL000000000000","expirationDate":"2025-01-01T00:00:00Z"}', 'SummaryCredential', '2023-06-29 13:49:11.870000', '2023-06-29 13:49:11.925000', null);
    INSERT INTO public.holders_credential (id, holder_did, issuer_did, credential_id, data, type, created_at, modified_at, modified_from) VALUES (3, 'did:web:localhost%3A8000:BPNL000000000042', 'did:web:localhost%3A8000:BPNL000000000042', 'a043c406-d51d-4672-ad89-517c68d025f9', '{"issuanceDate":"2023-06-29T13:49:11Z","credentialSubject":[{"holderIdentifier":"BPNL000000000000","id":"did:web:localhost%3A8000:BPNL000000000000","type":"SummaryCredential","items":["BpnCredential"],"contractTemplates":"https://public.catena-x.org/contracts/"}],"id":"a043c406-d51d-4672-ad89-517c68d025f9","proof":{"assertionMethod":"did:web:localhost%3A8000:BPNL000000000000#","proofPurpose":"proofPurpose","type":"JsonWebSignature2020","created":"2023-06-29T13:49:11Z","jws":"eyJhbGciOiJFZERTQSJ9..waFlAQyE42TddNz0v4q_MkIbSgbjexDQqdt-k6LEQG3BvhE1Adj6SIERtUzmYowuLtdcTBbwVAROf9yzpfNMAg"},"type":["VerifiableCredential","SummaryCredential"],"@context":["https://www.w3.org/2018/credentials/v1","https://catenax-ng.github.io/product-core-schemas/SummaryVC.json"],"issuer":"did:web:localhost%3A8000:BPNL000000000000","expirationDate":"2025-01-01T00:00:00Z"}', 'SummaryCredential', '2023-06-29 13:49:11.870000', '2023-06-29 13:49:11.925000', null);
EOSQL