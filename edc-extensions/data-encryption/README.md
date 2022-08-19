# Data Encryption Extension

The Eclipse Dataspace Connector encrypts sensitive information inside a token it sends to other applications (from possibly other companies). This extension implements the encryption of this data and should be used with secure keys and algorithms at all times.

## Configuration

| Key                                         | Description                                                                                                      | Mandatory | Default          |
|:--------------------------------------------|:-----------------------------------------------------------------------------------------------------------------|-----------|------------------|
| edc.data.encryption.keys.alias              | Keys for encryption and decryption of the data must be stored in the Vault under the configured alias.           | X         |                  |
| edc.data.encryption.algorithm               | Algorithm for encryption and decryption. Must be ether 'AES' or 'NONE'.                                          |           | AES              |
| edc.data.encryption.caching.enabled         | Enable caching to request only keys from the vault after the cache expires.                             |           | false            |
| edc.data.encryption.caching.seconds         | Duration in seconds until the cache expires.                                                                     |           | 3600             |

## Strategies

### 1. AES

The Advanced Encryption Standard (AES) is the default encryption algorithm. For Authenticated Encryption with Associated Data (AEAD) it uses the Galois/Counter Mode or GCM.

When using AES-GCM the key length must be ether 128-, 196- or 256bit. Keys must be stored stored Base64 encoded in the Vault, separated by a comma.

It's possible to generate Keys using OpenSSL
```bash
# 128 Bit
openssl rand -base64 16

# 196 Bit
openssl rand -base64 24

# 256 Bit
openssl rand -base64 32
```


### 2. NONE

This strategy does apply no encryption at all and should only be used for debugging purposes. Using NONE encryption may leak sensitive data to other connectors!