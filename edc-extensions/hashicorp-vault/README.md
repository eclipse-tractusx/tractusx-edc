# [HashiCorp Vault](https://www.vaultproject.io/) Extension

---

**Please note:**<br>
Using the HashiCorp vault it is possible to define multiple data entries per secret. Other vaults might allow only one
entry per secret (e.g. Azure Key Vault).

Therefore, the HashiCorp vault extension **only** checks the '**content**' data entry! Please use this knowledge when
creating secrets the EDC should consume.

---

## Configuration

| Key                                         | Description                                                                                                      | Mandatory | Default          |
|:--------------------------------------------|:-----------------------------------------------------------------------------------------------------------------|-----------|------------------|
| edc.vault.hashicorp.url                     | URL to connect to the HashiCorp Vault                                                                            | X         ||     |
| edc.vault.hashicorp.token                   | Value for [Token Authentication](https://www.vaultproject.io/docs/auth/token) with the vault                     | X         ||     |
| edc.vault.hashicorp.timeout.seconds         | Request timeout in seconds when contacting the vault                                                             |           | `30`             |
| edc.vault.hashicorp.health.check.enabled    | Enable health checks to ensure vault is initialized, unsealed and active                                         |           | `true`           |
| edc.vault.hashicorp.health.check.standby.ok | Specifies if a vault in standby is healthy. This is useful when Vault is behind a non-configurable load balancer |           | `false`          |
| edc.vault.hashicorp.api.secret.path         | Path to the [secret api](https://www.vaultproject.io/api-docs/secret/kv/kv-v1)                                   |           | `/v1/secret`     |
| edc.vault.hashicorp.api.health.check.path   | Path to the [health api](https://www.vaultproject.io/api-docs/system/health)                                     |           | `/v1/sys/health` |

## Health Check

The HashiCorp Vault Extension is able to run health checks. A health check is successful when the vault is _initialized_, _active_ and _unsealed_. Successful health checks are logged with level _FINE_. Unsuccessful health checks will be logged 
with level _WARNING_.

---
**Health Checks in Catena-X**

If your project uses the Catena-X HashiCorp Vault please set `edc.vault.hashicorp.health.check.standby.ok` to _true_. Otherwise the health check would fail if the Vault is in standby.
```bash
  # Logs of successful check with standby vault
  [2022-08-01 14:48:37] [FINE   ] HashiCorp Vault HealthCheck successful. HashicorpVaultHealthResponsePayload(isInitialized=true, isSealed=false, isStandby=true, isPerformanceStandby=false, replicationPerformanceMode=disabled,replicationDrMode=disabled, serverTimeUtc=1659365317, version=1.9.2, clusterName=vault-cluster-4b193c26, clusterId=83fabd45-685d-7f8d-9495-18fab6f50d5e)
```

---

## Example: Create & Configure DAPS Key

1. Insert DAPS Key into HashiCorp Vault

```bash
cat << EOF | /bin/vault kv put secret/my-daps-key content=-
        -----BEGIN PRIVATE KEY-----
        MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCv+NUvK7ppJPiM
        wZPaQQxE745T5pV38O/Mkay5m82nnd5BoMoCdhhRTy3Efy79FhvBfGruFBLLGzsQ
        FOEUY53Albeumo2gmpZSKjJR/M2ifK4MTaRniVOWL5mEcZSKPhsItKpxdLaiYfB6
        8uzqkqNICtmAQRSclYKzLBM9xHLEtxDWCbnzYFCHoOELGi+PTNIFsUnsT3QuKaJ/
        ejb47vdA/EZbwCQdtTyJ6i54jGhZUp0WMwq1Go2uhzJsygPmT2da/ZZZc7BNNEQE
        sUSMZSpMH807TG/TunstotrzO4ShhpV4zbJ2FV/VlxH7yuCawmnR84F/KnXs9fUc
        RSrQfuYBAgMBAAECggEAO+KjsjTgcG3bhBNQnMLsSP15Y0Yicbn18ZlVvaivGS7Z
        d14fwSytY+ZdPfTGaey/L16HCVSdfK9cr0Fbw9OO2P5ajzobnp9dLsMbctlkpbpm
        hNtbarzKTF8QkIkSsuUl0BWjt46vpJ1N+Jl5VO7oUFkY4dPEDvG2lAEY3zlekWDm
        cQeOC/YgpoW4xfRwPPS6QE0w3Q+H5NfNjfz+mSHeItTlVfTKDRliWQLPWeRZFuXh
        FlRFUQnTmEE/9wpIe3Hn7WXJ3fQqcYDzxU7/zwwY9I7bB15SgVHlR0ENDPAD5X8F
        MVZ3EcLlqGBy+WvTWALp6pc8YfhW3fiTWyuamXtNrQKBgQDonsIzBKEOOKdKGW0e
        uyw79ErmnmzkY5nuMrMxrmTA4WKCfJ/YRRA+4sxiltWsIJ3UkHe3OBCSSCdj79hb
        ugb/+UzE70hOdgrct2NUQqbrj3gvsVvU8ZRQgTRMqKpmC0zY7KOMx6NU85z3IvS1
        z5fjszcUv4kLQlldYGSAuqPy+wKBgQDBqIkc8p/wcw7ygo1q/GerNeszfoxiIFp8
        h4RWLVhkwrcXFz30wBlUWuv5/kxU8tmJcmXxe72EmUstd6wvNOAnYwCiile6zQiJ
        vsr1axavZnGOtNGUp6DUAsd2iviBl7IZ7kAcqCrQo4ivGhfHmahH3hmg8wuAMjYB
        8f+FSPgaMwKBgQC7W4tMrjDOFIFhJEOIWfcRvvxI7VcFSNelS76aiDzsQVwnfxr7
        hPzFucQmsBgfUBHvMADMWGK4f1cCnh5kGtwidXgIsjVJxLeQ+EAPkLOCzQZfW3l8
        dKshgD9QcxTzpaxal5ZPAEikVqaZQtVYToCmzCTUGETYBbOWitnH+Qut2wKBgQC6
        Y6DcSLUhc0xOotLDxv1sbu/aVxF8nFEbDD+Vxf0Otc4MnmUWPRHj+8KlkVkcZcR0
        IrP1kThd+EDAGS+TG9wmbIY+6tH3S8HM+eJUBWcHGJ1xUZ1p61DC3Y3nDWiTKlLT
        3Fi+fCkBOHSku4Npq/2odh7Kp0JJd4o9oxJg0VNhuwKBgQDSFn7dqFE0Xmwc40Vr
        0wJH8cPWXKGt7KJENpj894buk2DniLD4w2x874dzTjrOFi6fKxEzbBNA9Rq9UPo8
        u9gKvl/IyWmV0c4zFCNMjRwVdnkMEte/lXcJZ67T4FXZByqAZlhrr/v0FD442Z9B
        AjWFbUiBCFOo+gpAFcQGrkOQHA==
        -----END PRIVATE KEY-----
        EOF
```

2. Configure Key in the EDC

```bash
 EDC_OAUTH_PRIVATE_KEY_ALIAS: my-daps-key
```

or

```bash
 edc.oauth.private.key.alias=my-daps-key
```