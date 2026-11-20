# Create a dataspace extension

Every dataspace has its own rules: which credentials prove membership, which policy constraints partners may use,
how participants are identified. In Tractus-X EDC these rules are **extensions**. The Catena-X rules are the
`cx-*` extensions; this page shows how to write the same for another dataspace, called *Example-X* here.

Read [Supported dataspaces](index.md) first for the core and flavour model, and
[Tutorial 4: your first extension](../tutorials/first-extension.md) if you have never written an EDC extension.

## 1. What a dataspace usually adds

| Rule of the dataspace | Extension point | Catena-X counterpart |
|---|---|---|
| Policy constraints (membership, framework agreements, usage purposes) | Policy functions on the `PolicyEngine`, bound with the `RuleBindingRegistry` | `cx-policy` |
| Credentials to request from a partner | `ScopeExtractor` registered with the `ScopeExtractorRegistry` | `cx-dcp` |
| Credentials requested on every call | Default scopes, a post-validator on the request policy contexts | `cx-dcp` (`CxDcpDefaultScopeExtension`) |
| Participant identifiers other than the DID | An identity mapper such as a directory client | `bdrs-client`, `bpn-validation` |
| JSON-LD vocabulary for policies | A JSON-LD context cached in the runtime | `core/json-ld-cx` |

Keep anything that every dataspace needs out of your extension and in the generic core, so it can be shared.

## 2. Create the module

Name the module after the dataspace, the way Catena-X uses `cx-`: for example `ex-policy`. Develop it in your own
repository first; the [ADR](../development/decision-records/2026-01-07_multi-dataspace-support/README.md) brings new
flavours into this repository once they reach production status.

```kotlin title="build.gradle.kts"
plugins {
    `java-library`
}

dependencies {
    implementation("org.eclipse.edc:boot-spi:<edc-version>")                    // ServiceExtension
    implementation("org.eclipse.edc:runtime-metamodel:<edc-version>")           // @Extension, @Inject
    implementation("org.eclipse.edc:policy-engine-spi:<edc-version>")           // PolicyEngine, rule functions
    implementation("org.eclipse.edc:participant-spi:<edc-version>")             // ParticipantAgentPolicyContext
    implementation("org.eclipse.edc:catalog-spi:<edc-version>")                 // CatalogPolicyContext
    implementation("org.eclipse.edc:contract-spi:<edc-version>")                // negotiation and transfer contexts
    implementation("org.eclipse.edc:request-policy-context-spi:<edc-version>")  // RequestPolicyContext
    implementation("org.eclipse.edc:decentralized-claims-spi:<edc-version>")    // ScopeExtractor
    implementation("org.eclipse.edc:verifiable-credentials-spi:<edc-version>")  // VerifiableCredential
}
```

Use the EDC version that the Tractus-X EDC release you build on pins (`edc` in
[`gradle/libs.versions.toml`](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/gradle/libs.versions.toml)).

## 3. A policy constraint

An `Example-X` membership constraint, `https://w3id.org/example-x/policy/Membership eq active`, that passes when the
partner presented an `ExampleXMembershipCredential`:

```java
public class ExampleXMembershipFunction<C extends ParticipantAgentPolicyContext>
        implements AtomicConstraintRuleFunction<Permission, C> {

    static final String CREDENTIAL_TYPE = "ExampleXMembershipCredential";

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, C context) {
        if (operator != Operator.EQ || !"active".equals(rightValue)) {
            context.reportProblem("Membership only supports 'eq active'");
            return false;
        }
        var credentials = context.participantAgent().getClaims().get("vc");   // (1)!
        return credentials instanceof List<?> list && list.stream()
                .filter(VerifiableCredential.class::isInstance)
                .map(VerifiableCredential.class::cast)
                .anyMatch(vc -> vc.getType().contains(CREDENTIAL_TYPE));
    }
}
```

1.  The verified credentials of the partner, put there by the DCP extension after checking signatures, issuers and
    revocation.

Register it for the policy scopes in which it is evaluated (catalog, negotiation, transfer) and bind the constraint
key to them, so that the engine evaluates it:

```java
@Extension(ExampleXPolicyExtension.NAME)
public class ExampleXPolicyExtension implements ServiceExtension {

    static final String NAME = "Example-X policies";
    static final String MEMBERSHIP = "https://w3id.org/example-x/policy/Membership";

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry bindingRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        for (var scope : List.of("catalog", "contract.negotiation", "transfer.process")) {
            bindingRegistry.bind(MEMBERSHIP, scope);
        }
        policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class, MEMBERSHIP,
                new ExampleXMembershipFunction<>());
        policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, MEMBERSHIP,
                new ExampleXMembershipFunction<>());
        policyEngine.registerFunction(TransferProcessPolicyContext.class, Permission.class, MEMBERSHIP,
                new ExampleXMembershipFunction<>());
    }
}
```

List the class in `src/main/resources/META-INF/services/org.eclipse.edc.spi.system.ServiceExtension`, so the runtime
finds it.

## 4. Ask for the right credentials

The provider can only evaluate the constraint if the consumer sends the credential. A `ScopeExtractor` tells the
consumer's connector which DCP scope a policy needs:

```java
public class ExampleXScopeExtractor implements ScopeExtractor {

    private static final String SCOPE_ALIAS = "org.example-x.vc.type";   // (1)!

    @Override
    public Set<String> extractScopes(Object leftValue, Operator operator, Object rightValue,
                                     RequestPolicyContext context) {
        return ExampleXPolicyExtension.MEMBERSHIP.equals(leftValue)
                ? Set.of(SCOPE_ALIAS + ":ExampleXMembershipCredential:read")
                : Set.of();
    }
}
```

1.  The scope alias your dataspace's wallets understand. Catena-X uses `org.eclipse.tractusx.vc.type`.

Register it in an extension with `scopeExtractorRegistry.registerScopeExtractor(new ExampleXScopeExtractor())`, as
`CxDcpScopeExtractorExtension` does.

## 5. Build a runtime for your dataspace

An extension is active only in a runtime that has it on its classpath. Tractus-X EDC publishes its modules to Maven
Central as `org.eclipse.tractusx.edc:<module>:<version>`. Until the ADR's `core` runtimes exist, start from the
Catena-X runtimes and leave out the Catena-X modules you replace:

```kotlin title="my-controlplane/build.gradle.kts"
dependencies {
    runtimeOnly("org.eclipse.tractusx.edc:edc-controlplane-base:<tractusx-edc-version>") {
        exclude(group = "org.eclipse.tractusx.edc", module = "cx-policy")       // replaced by ex-policy
    }
    runtimeOnly(project(":ex-policy"))
}
```

Exclude carefully: other modules may depend on what you remove, so run the end-to-end tests of your runtime. The
[Factory-X EDC](https://github.com/factory-x-contributions/factoryx-edc) builds its runtimes this way and is a
working example to learn from.

## 6. Test and share

- Unit-test each function with EDC's `DependencyInjectionExtension`, as in [Testing](../developer/testing.md).
- Run two connectors of your flavour and negotiate a contract that uses your constraint.
- Generic improvements you need on the way (a split of an extension that is still Catena-X specific) belong in this
  repository: open an issue or a pull request, see [Contribution workflow](../developer/contributing.md).
- Tell us about it: add your connector to [Known friends](../home/known-friends.md) and present it in the
  [Connector Open Meeting](https://eclipse-tractusx.github.io/community/open-meetings#edc-weekly).

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- AI Disclosure: This file was entirely AI-generated. The AI-generated portions are made available under CC0-1.0 and
  not subject to the project's license. The human contributor has reviewed and verified that the content is correct.
- SPDX-License-Identifier: CC-BY-4.0 AND CC0-1.0
- SPDX-FileCopyrightText: 2026 Catena-X Automotive Network e.V.
- SPDX-FileCopyrightText: 2026 Contributors to the Eclipse Foundation
- Assisted-By: Claude Code (claude-opus-5-5)
- Source URL: <https://github.com/eclipse-tractusx/tractusx-edc>
