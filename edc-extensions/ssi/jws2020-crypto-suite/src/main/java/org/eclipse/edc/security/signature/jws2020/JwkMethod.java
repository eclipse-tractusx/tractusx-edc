package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.signature.key.KeyPair;
import com.nimbusds.jose.jwk.JWK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (JwkMethod) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.controller, that.controller) &&
                Objects.equals(this.keyPair, that.keyPair);
    }

    @Override
    public String toString() {
        return "JwkMethod[" +
                "id=" + id + ", " +
                "type=" + type + ", " +
                "controller=" + controller + ", " +
                "keyPair=" + keyPair + ']';
    }

}
