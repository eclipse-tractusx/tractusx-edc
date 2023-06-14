package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.signature.KeyGenError;
import com.apicatalog.ld.signature.SigningError;
import com.apicatalog.ld.signature.VerificationError;
import com.apicatalog.ld.signature.algorithm.SignatureAlgorithm;
import com.apicatalog.ld.signature.key.KeyPair;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import org.eclipse.edc.spi.EdcException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.util.Collections;

class Jws2020SignatureProvider implements SignatureAlgorithm {

    @Override
    public void verify(byte[] publicKey, byte[] signature, byte[] data) throws VerificationError {

        try {
            var jwk = deserialize(publicKey);
            if (jwk == null) {
                throw new UnsupportedOperationException("Cannot deserialize public key, expected JWK format");
            }
            var verifier = KeyFactory.createVerifier(jwk);

            var detachedPayload = new Payload(data);
            var jws = new String(signature);

            var parsedJwsObject = JWSObject.parse(jws, detachedPayload);
            var isValid = parsedJwsObject.verify(verifier);

            if (!isValid) {
                throw new VerificationError(VerificationError.Code.InvalidSignature);
            }

        } catch (JOSEException | ParseException e) {
            throw new VerificationError(VerificationError.Code.InvalidSignature, e);
        }
    }

    @Override
    public byte[] sign(byte[] privateKey, byte[] data) throws SigningError {

        try {
            var keyPair = deserialize(privateKey);
            if (keyPair == null) {
                throw new UnsupportedOperationException("Cannot deserialize key pair, expected JWK format");
            }
            // Create and sign JWS
            JWSHeader header = new JWSHeader.Builder(from(keyPair))
                    .base64URLEncodePayload(false)
                    .criticalParams(Collections.singleton("b64"))
                    .build();

            var detachedPayload = new Payload(data);
            var jwsObject = new JWSObject(header, detachedPayload);
            jwsObject.sign(KeyFactory.createSigner(keyPair));

            boolean isDetached = true;
            String jws = jwsObject.serialize(isDetached);
            return jws.getBytes();

        } catch (JOSEException e) {
            throw new SigningError(SigningError.Code.UnsupportedCryptoSuite, e);
        }
    }

    @Override
    public KeyPair keygen(int length) throws KeyGenError {
        return null;
    }

    /**
     * Attempt to determine the {@link JWSAlgorithm} from the curve that is being used in the ECKey pair
     */
    private JWSAlgorithm from(JWK keyPair) {
        if (keyPair instanceof ECKey eckey) {
            var jwsAlgorithm = JWSAlgorithm.Family.EC.stream()
                    .filter(algo -> Curve.forJWSAlgorithm(algo).contains(eckey.getCurve()))
                    .findFirst();
            return jwsAlgorithm.orElseThrow(() -> new EdcException("Could not determine JWSAlgorithm for Curve " + eckey.getCurve()));
        } else if (keyPair instanceof OctetKeyPair okp) {
            var jwsAlgorithm = JWSAlgorithm.Family.ED.stream()
                    .filter(algo -> Curve.forJWSAlgorithm(algo).contains(okp.getCurve()))
                    .findFirst();
            return jwsAlgorithm.orElseThrow(() -> new EdcException("Could not determine JWSAlgorithm for Curve " + okp.getCurve()));
        } else if (keyPair instanceof RSAKey rsaKey) {
            // todo: figure out jws algorithm for rsa key
            //            var jwsAlgorithm = JWSAlgorithm.Family.RSA.stream()
            //                    .filter(algo -> rsaKey.)
            //                    .findFirst();
            //            return jwsAlgorithm.orElseThrow(() -> new EdcException("Could not determine JWSAlgorithm for RSA Key "));
        }
        return null;
    }

    private JWK deserialize(byte[] privateKey) {
        ByteArrayInputStream bis = new ByteArrayInputStream(privateKey);
        try {
            ObjectInputStream in = new ObjectInputStream(bis);
            return (JWK) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
