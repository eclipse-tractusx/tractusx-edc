package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.signature.key.KeyPair;
import com.nimbusds.jose.jwk.JWK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;

record JwkMethod(URI id, URI type, URI controller, JWK keyPair) implements KeyPair {

    @Override
    public byte[] privateKey() {
        return keyPair != null ? serializeKeyPair(keyPair) : null;
    }


    @Override
    public byte[] publicKey() {
        return keyPair != null ? serializeKeyPair(keyPair.toPublicJWK()) : null;
    }

    private byte[] serializeKeyPair(JWK keyPair) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(keyPair);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
