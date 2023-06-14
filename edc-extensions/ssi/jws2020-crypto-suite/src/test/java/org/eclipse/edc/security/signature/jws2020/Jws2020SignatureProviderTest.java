package org.eclipse.edc.security.signature.jws2020;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Jws2020SignatureProviderTest {

    @Test
    void test() throws JOSEException {
        // Generate a key pair with Ed25519 curve
        var jwk = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        var publicJwk = jwk.toPublicJWK();
        System.out.println("OKP: " + publicJwk.toJSONString());

        var signer = new Ed25519Signer(jwk);
        var payload = "foobar";
        var jwsObject = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .base64URLEncodePayload(false)
                .criticalParams(Set.of("b64"))
                .build(),
                new Payload(payload));

        jwsObject.sign(signer);

        var s = jwsObject.serialize(true);
        System.out.println("JWS: " + s);


        var verifier = new Ed25519Verifier(publicJwk);

        assertThat(jwsObject.verify(verifier)).isTrue();
        assertThat(jwsObject.getPayload().toString()).isEqualTo(payload);
    }

}