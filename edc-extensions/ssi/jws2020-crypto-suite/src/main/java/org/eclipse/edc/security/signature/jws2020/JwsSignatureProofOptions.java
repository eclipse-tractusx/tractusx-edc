package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.signature.SignatureSuite;
import com.apicatalog.vc.integrity.DataIntegrityProofOptions;

/**
 * Proof options for Jws2020
 */
public class JwsSignatureProofOptions extends DataIntegrityProofOptions {
    /**
     * Create a new proof options instance
     * @param suite The {@link JwsSignature2020Suite} for which the options are created.
     */
    public JwsSignatureProofOptions(JwsSignature2020Suite suite) {
        super(suite);
    }
}
