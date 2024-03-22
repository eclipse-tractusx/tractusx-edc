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

package org.eclipse.tractusx.edc.tests.miw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.failsafe.RetryPolicy;
import jakarta.json.Json;
import okhttp3.OkHttpClient;
import org.eclipse.edc.iam.oauth2.client.Oauth2ClientImpl;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.http.client.EdcHttpClientImpl;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2ClientConfiguration;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2ClientImpl;
import org.eclipse.tractusx.edc.tag.MiwIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@MiwIntegrationTest
public class WalletTest {

    static final String MIW_SOKRATES_URL = "http://localhost:8000";
    static final String OAUTH_TOKEN_URL = "http://localhost:8080/realms/miw_test/protocol/openid-connect/token";
    private static final ObjectMapper OBJECT_MAPPER = JacksonJsonLd.createObjectMapper();
    private static final String OTHER_PARTICIPANTS_DID = "did:web:localhost%3A8000:BPNL000000000042";
    private final TypeReference<Map<String, Object>> mapRef = new TypeReference<>() {
    };
    private MiwApiClient client;

    @BeforeEach
    void setup() {

        var monitor = mock(Monitor.class);
        var httpClient = new EdcHttpClientImpl(new OkHttpClient.Builder().build(), RetryPolicy.ofDefaults(), monitor);
        var config = MiwOauth2ClientConfiguration.Builder.newInstance()
                .clientId("miw_private_client")
                .clientSecret("miw_private_client")
                .tokenUrl(OAUTH_TOKEN_URL)
                .build();
        var oauth2baseClient = new Oauth2ClientImpl(httpClient, new TypeManager());

        var auth2client = spy(new MiwOauth2ClientImpl(oauth2baseClient, config));

        this.client = new MiwApiClientImpl(httpClient, MIW_SOKRATES_URL, auth2client, "miw_private_client", "BPNL000000000000", OBJECT_MAPPER, monitor);
    }

    @Test
    @DisplayName("t0001: Request VC, create a VP and verify")
    void requestAndVerifyVp() {
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        assertThat(credentialsResult.succeeded()).withFailMessage(credentialsResult::getFailureDetail).isTrue();
        assertThat(credentialsResult.getContent()).isNotEmpty();

        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");
        assertThat(presentationResult.succeeded()).isTrue();

        var vp = presentationResult.getContent().get("vp").toString();
        var verifyResult = client.verifyPresentation(vp, "test-audience");

        assertThat(verifyResult.succeeded()).describedAs("Should be able to verify its own VP: " + verifyResult.getFailureDetail()).isTrue();
    }

    @Test
    @DisplayName("t0002: A wrong audience (passed to API) is rejected")
    void verifyVp_withWrongAudience() {
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));

        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var verifyResult = client.verifyPresentation(presentationResult.getContent().get("vp").toString(), "wrong-audience");

        assertThat(verifyResult.failed()).describedAs("Should not be able to verify against a wrong audience").isTrue();
    }

    @Test
    @DisplayName("t0003: A a spoofed/self-signed VP is rejected")
    void verifyVp_spoofedVp() throws ParseException, JOSEException {
        // obtain VC, create VP
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var vpToken = presentationResult.getContent().get("vp").toString();

        // extract content, parse into JWSobject
        var jwt = JWSObject.parse(vpToken);

        // create new JWS header
        var header = createHeader();

        // sign JWS with own key
        var claimsSet = new JWTClaimsSet.Builder();
        jwt.getPayload().toJSONObject().forEach(claimsSet::claim);
        var jwk = createJwkKeypair();

        var ownJwt = new SignedJWT(header, claimsSet.build());
        ownJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        // this should definitely return false!!
        var result = client.verifyPresentation(ownJwt.serialize(), "test-audience");

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    @DisplayName("t0004: A completely bogus JWT (without even a VP inside) is rejected")
    void verifyVp_bogusTokenWithoutVp() throws JOSEException {

        // extract content, parse into JWSobject
        // create new JWS header
        var header = createHeader();

        // generate completely arbitrary JWT claims
        var claimsSet = new JWTClaimsSet.Builder();
        claimsSet.claim("aud", "test-audience")
                .claim("iss", OTHER_PARTICIPANTS_DID);
        var jwk = createJwkKeypair();

        var ownJwt = new SignedJWT(header, claimsSet.build());
        ownJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        // this should definitely return false!!
        var result = client.verifyPresentation(ownJwt.serialize(), "test-audience");

        assertThat(result.succeeded()).isFalse();
    }

    @Test
    @DisplayName("t0005: A forged VC proof (altered JWS) is rejected")
    void verifyVp_spoofedVpAndForgedJws() throws JsonProcessingException, ParseException, JOSEException {
        // obtain VC, create VP
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var vpToken = presentationResult.getContent().get("vp").toString();
        // extract content, parse into JWSobject
        var jwt = JWSObject.parse(vpToken);

        // create new JWS header
        var header = createHeader();

        // sign JWS with own key
        var claimsSet = new JWTClaimsSet.Builder();


        var payloadJson = jwt.getPayload().toJSONObject();
        var jo = Json.createObjectBuilder(payloadJson).build();

        // replace JWS inside the VC's proof object
        var jws = jo.getJsonObject("vp").getJsonArray("verifiableCredential").getJsonObject(0).getJsonObject("proof").getString("jws");
        var invalidJws = jws.replace("a", "X");
        var tamperedJson = jo.toString().replace(jws, invalidJws);
        var tamperedJsonObject = OBJECT_MAPPER.readValue(tamperedJson, mapRef);
        tamperedJsonObject.forEach(claimsSet::claim);
        var jwk = createJwkKeypair();

        var forgedJwt = new SignedJWT(header, claimsSet.build());
        forgedJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        // this should definitely return false!!
        var result = client.verifyPresentation(forgedJwt.serialize(), "test-audience");

        assertThat(result.succeeded()).withFailMessage("Verifying a forged VC proof (JWS) should not be possible!").isFalse();
    }

    @Test
    @DisplayName("t0006: A tampered VC proof (changed holderIdentifier) is rejected")
    void verifyVp_spoofedVpAndTamperedVc() throws JsonProcessingException, ParseException, JOSEException {
        // obtain VC, create VP
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var vpToken = presentationResult.getContent().get("vp").toString();

        // extract content, parse into JWSobject
        var jwt = JWSObject.parse(vpToken);

        // create new JWS header
        var header = createHeader();

        // sign JWS with own key
        var claimsSet = new JWTClaimsSet.Builder();


        var payloadJson = jwt.getPayload().toString();
        var tamperedPayload = payloadJson.replace("\"holderIdentifier\":\"BPNL000000000000\"", "\"holderIdentifier\":\"wrongHolderIdentifier\"");
        var tamperedJsonObject = OBJECT_MAPPER.readValue(tamperedPayload, mapRef);
        tamperedJsonObject.forEach(claimsSet::claim);

        var jwk = createJwkKeypair();

        var forgedJwt = new SignedJWT(header, claimsSet.build());
        forgedJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        // this should definitely return false!!
        var result = client.verifyPresentation(forgedJwt.serialize(), "test-audience");

        assertThat(result.succeeded()).withFailMessage("Verifying a tampered holderIdentifier should not be possible!").isFalse();
    }

    @Test
    @DisplayName("t0007: An invalid 'iss' claim (use existing other issuer) is rejected")
    void verifyVp_invalidIssuerClaim() throws ParseException, JOSEException {
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var vpToken = presentationResult.getContent().get("vp").toString();

        // extract content, parse into JWSobject
        var jwt = JWSObject.parse(vpToken);

        // create new JWS header
        var header = createHeader();

        // replace payload
        var claimsSet = new JWTClaimsSet.Builder();

        var vp = jwt.getPayload().toJSONObject();
        // change the iss claim - please check the src/test/resources/db.sh script, this additional participant is added there
        vp.replace("iss", OTHER_PARTICIPANTS_DID);

        // create new VP token
        vp.forEach(claimsSet::claim);

        var jwk = createJwkKeypair();
        var forgedJwt = new SignedJWT(header, claimsSet.build());
        forgedJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        var result = client.verifyPresentation(forgedJwt.serialize(), "test-audience");

        assertThat(result.succeeded()).withFailMessage("Altering the 'iss' claim (impersonation) should not be possible!").isFalse();
    }

    @Test
    @DisplayName("t0008: An invalid 'iss' claim (non-existing issuer) is rejected")
    void verifyVp_invalidIssuerClaim_notExists() throws ParseException, JOSEException {
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var vpToken = presentationResult.getContent().get("vp").toString();

        // extract content, parse into JWSobject
        var jwt = JWSObject.parse(vpToken);

        // create new JWS header
        var header = createHeader();

        // replace payload
        var claimsSet = new JWTClaimsSet.Builder();

        var vp = jwt.getPayload().toJSONObject();
        // change the iss claim - please check the src/test/resources/db.sh script, this additional participant is added there
        vp.replace("iss", "did:web:someotherissuer");

        // create new VP token
        vp.forEach(claimsSet::claim);

        var jwk = createJwkKeypair();
        var forgedJwt = new SignedJWT(header, claimsSet.build());
        forgedJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        var result = client.verifyPresentation(forgedJwt.serialize(), "test-audience");

        assertThat(result.succeeded()).withFailMessage("Altering the 'iss' claim (impersonation) should not be possible!").isFalse();
    }

    @Test
    @DisplayName("t0009: An invalid 'iss' claim (not in DID:web format) is rejected")
    void verifyVp_invalidIssuerClaim_notExists_issNotDidWeb() throws ParseException, JOSEException {
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var vpToken = presentationResult.getContent().get("vp").toString();

        // extract content, parse into JWSobject
        var jwt = JWSObject.parse(vpToken);

        // create new JWS header
        var header = createHeader();

        // replace payload
        var claimsSet = new JWTClaimsSet.Builder();

        var vp = jwt.getPayload().toJSONObject();
        // change the iss claim - please check the src/test/resources/db.sh script, this additional participant is added there
        vp.replace("iss", "this_isnt_even_didweb");

        // create new VP token
        vp.forEach(claimsSet::claim);

        var jwk = createJwkKeypair();
        var forgedJwt = new SignedJWT(header, claimsSet.build());
        forgedJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        var result = client.verifyPresentation(forgedJwt.serialize(), "test-audience");

        assertThat(result.succeeded()).withFailMessage("Altering the 'iss' claim (impersonation) should not be possible!").isFalse();
    }

    @Test
    @DisplayName("t0010: An altered 'aud' claim is rejected")
    void verifyVp_modifiedAudClaim() throws ParseException, JOSEException {
        var credentialsResult = client.getCredentials(Set.of("SummaryCredential"));
        var presentationResult = client.createPresentation(credentialsResult.getContent(), "test-audience");

        var vpToken = presentationResult.getContent().get("vp").toString();

        // extract content, parse into JWSobject
        var jwt = JWSObject.parse(vpToken);

        // create new JWS header
        var header = createHeader();

        // replace payload
        var claimsSet = new JWTClaimsSet.Builder();

        var vp = jwt.getPayload().toJSONObject();
        // change the iss claim
        vp.replace("aud", "some-other-audience");

        // create new VP token
        vp.forEach(claimsSet::claim);

        var jwk = createJwkKeypair();
        var forgedJwt = new SignedJWT(header, claimsSet.build());
        forgedJwt.sign(new Ed25519Signer(jwk.toOctetKeyPair()));

        var result = client.verifyPresentation(forgedJwt.serialize(), "some-other-audience");

        assertThat(result.succeeded()).withFailMessage("Altering the 'aud' claim (replay attack) should not be possible!").isFalse();
    }

    private JWSHeader createHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .type(JOSEObjectType.JWT)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private JWK createJwkKeypair() {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key (optional)
                    .keyID(UUID.randomUUID().toString()) // give the key a unique ID (optional)
                    .issueTime(new Date()) // issued-at timestamp (optional)
                    .generate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
