# Release Notes Version 0.1.5

13.02.2023

## 0. Summary

1. [Version updates](#1-version-updates)
    - Use patched EDC version: 0.0.1-20220922.2-SNAPSHOT
2. [Extensions](#2-extensions)
    - [2.1 Data Encryption Extension](#22-data-encryption-extension)
        - Fixed usage of a blocking algorithm

## 1. Version Updates

## 1.1 Use patched EDC version: 0.0.1-20220922.2-SNAPSHOT

The version has been updated to the patched version `0.0.1-20220922.2-SNAPSHOT` that brings in a bugfix regarding the
catalog pagination. [GitHub issue](https://github.com/eclipse-edc/Connector/issues/2008)

## 2. Extensions

### 2.2 Data Encryption Extension

The encryption of the `EndpointDataReference` took up to 3 minutes unter certain circumstances.
This was fixed by using a not blocking algorithm and setting the Java CMD flag `java.security.egd` correctly.