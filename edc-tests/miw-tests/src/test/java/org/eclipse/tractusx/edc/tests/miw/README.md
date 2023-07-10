# Function-testing the Managed-Identity-Wallet

## Test setup

As test subject we used a `docker-compose.yml` file located in `src/main/resources/`. From that directory, simply
execute `docker compose up --wait`, and then, once everything is started,
run `docker exec -i resources-postgres-1 /opt/seed.sh` to seed test data.

## Test suite description

### `t0001` Request and verify a VP

### `t0002` Wrong audience

This test asserts, that a verification request is rejected, if the wrong `audience=` query parameter is supplied.
The `audience` query parameter must match the `aud` claim inside the token.

### `t0003` A self-signed VP token is rejected

This test asserts, that submitting a self-generated JWT (containing the original VP claim) should be rejected. The MIW
should only accept JWTs that were signed by the requestor's private key, which is hosted in MIW. Currently, no JWT
validation is done.

A rejected flow would be:

- request VC from MIW
- request VP from MIW, returned in JWT format
- decode the JWT, unpack the payload
- generate a random keypair
- re-use the original claims (payload) and header
- sign with the random keypair

### `t0004` A bogus JWT is rejected

This test is an amendment to `t0003` in that it not only forges the JWT itself, but the JWT does not contain any of the
required claims. For example, it does not even contain a `vp` claim, so there is no VerifiablePresentation.

### `t0005` A forged VC proof (altered JWS) is rejected

This test asserts, that an altered (and potentially even malformed) `jws` proof is rejected. This test specifically
targets the use of JsonWebSignature2020, because there the `proof` object contains a `jws` field.

Altering that `jws` value, here by replacing all "a" with "X" should cause the MIW to reject the verification request.

### `t0006` A tampered VC proof (changed document) is rejected

Similar to `t0005`, which alters the proof itself, this test alters the document, for which the proof was created.
Technically this should alter the document hash, so the proof becomes invalid, and the MIW should reject the request.

### `t0007` Forged `iss` claim is rejected

In this test we construct an impersonation attack, which assumes there are at least two participants in the MIW.
Participant 1 requests a VP, decodes it, replaces the `iss` claim with the ID of Participant 2 and - using again a
randomly generated keypair - signs this forged VP token. This effectively gives any participant the possibility to mount
impersonation attacks.

> Note that Participant 2 was created in the database using the `src/test/resources/db.sh` script

### `t0008` Invalid `iss` claim is rejected (non-existent user)

This test attempts to have a JWT verified where the `iss` claim cannot be resolved.

### `t0009` Invalid `iss` claim is rejected (not did:web format)

This test asserts that a malformed `iss` claim is rejected by MIW. Specifically, the claim must be in `did:web:....`
format.

### `t0010` An altered `aud` claim is rejected

Similar to `t0007`, and in extension to `t0003`, this test asserts, that a verification request is rejected by MIW, if
the `aud` claim inside the JWT token was replaced.
> Note that this attack is only possible if the integrity and provenance of the JWT is not checked, see `t0003`.
