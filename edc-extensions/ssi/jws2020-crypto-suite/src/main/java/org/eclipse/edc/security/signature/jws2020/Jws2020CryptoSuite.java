package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.schema.LdTerm;
import com.apicatalog.ld.signature.CryptoSuite;
import com.apicatalog.ld.signature.primitive.MessageDigest;
import com.apicatalog.ld.signature.primitive.Urdna2015;

class Jws2020CryptoSuite extends CryptoSuite {
    Jws2020CryptoSuite(LdTerm id) {
        super(id, new Urdna2015(), new MessageDigest("SHA-256"), new Jws2020SignatureProvider());
    }
}
