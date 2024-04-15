package org.eclipse.tractusx.edc.identity.mapper;

public class TestData {
    public static final String MEMBERSHIP_CREDENTIAL = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/catenax/credentials/v1.0.0"
              ],
              "id": "1f36af58-0fc0-4b24-9b1c-e37d59668089",
              "type": [
                "VerifiableCredential",
                "MembershipCredential"
              ],
              "issuer": "did:web:com.example.issuer",
              "issuanceDate": "2021-06-16T18:56:59Z",
              "expirationDate": "2099-06-16T18:56:59Z",
              "credentialSubject": {
                "id": "%s",
                "holderIdentifier": "BPNL000000001"
              }
            }
            """;

    public static final String SOME_OTHER_CREDENTIAL = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/catenax/credentials/v1.0.0"
              ],
              "id": "1f36af58-0fc0-4b24-9b1c-e37d59668089",
              "type": [
                "VerifiableCredential",
                "SomeOtherCredential"
              ],
              "issuer": "did:web:com.example.issuer",
              "issuanceDate": "2021-06-16T18:56:59Z",
              "expirationDate": "2099-06-16T18:56:59Z",
              "credentialSubject": {
                "id": "%s",
                "holderIdentifier": "BPNL000000001"
              }
            }
            """;

    public static final String VP_CONTENT_EXAMPLE = """
                        {
                            "@context": [
                              "https://www.w3.org/2018/credentials/v1",
                              "https://www.w3.org/2018/credentials/examples/v1"
                            ],
                            "id": "https://exapmle.com/test-vp",
                            "holder": "%s",
                            "type": [
                              "VerifiablePresentation"
                            ],
                            "verifiableCredential": [
                              %s
                            ]
                        }
            """;
}
