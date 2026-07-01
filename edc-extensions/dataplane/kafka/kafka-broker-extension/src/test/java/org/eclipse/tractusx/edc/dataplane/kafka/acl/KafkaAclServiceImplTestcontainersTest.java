/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.dataplane.kafka.acl;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeAclsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.plain.internals.PlainSaslServer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class KafkaAclServiceImplTestcontainersTest {

    public static final String GROUP_ID = "groupId";
    private static final String TEST_TOPIC = "test-topic";
    private static final TopicPartition TEST_PARTITION = new TopicPartition(TEST_TOPIC, 0);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);
    private static final String TEST_OAUTH_SUBJECT = "test-user";
    // Deliberately distinct from the OAuth subject: the GROUP ACL is named by the group prefix (PREFIXED),
    // while the ACL principal remains the subject.
    private static final String TEST_GROUP_PREFIX = "test-group-prefix";
    private static final String TEST_TRANSFER_PROCESS_ID = "transfer-process-123";
    private static final String UNAUTHORIZED_USER = "unauthorized-user";

    private static final String ADMIN_LOGIN_MODULE = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"admin\" password=\"password\";";

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"))
            .withEnv("KAFKA_AUTHORIZER_CLASS_NAME", "org.apache.kafka.metadata.authorizer.StandardAuthorizer")
            .withEnv("KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND", "false")
            .withEnv("KAFKA_SUPER_USERS", "User:admin;User:ANONYMOUS")
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:SASL_PLAINTEXT,BROKER:PLAINTEXT,CONTROLLER:PLAINTEXT")
            .withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN")
            .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT")
            .withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN")
            .withEnv("KAFKA_SASL_JAAS_CONFIG", ADMIN_LOGIN_MODULE)
            .withEnv("KAFKA_LISTENER_NAME_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                            "user_admin=\"password\" user_test-user=\"password\" user_unauthorized-user=\"password\";");

    private KafkaAclServiceImpl aclService;
    private Admin adminClient;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        Properties adminProperties = new Properties();
        adminProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        adminProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name());
        adminProperties.put(SaslConfigs.SASL_MECHANISM, PlainSaslServer.PLAIN_MECHANISM);
        adminProperties.put(SaslConfigs.SASL_JAAS_CONFIG, ADMIN_LOGIN_MODULE);

        adminClient = Admin.create(adminProperties);

        Monitor monitor = new ConsoleMonitor();
        aclService = new KafkaAclServiceImpl(adminProperties, monitor);

        NewTopic newTopic = new NewTopic(TEST_TOPIC, 1, (short) 1);
        CreateTopicsResult createResult = adminClient.createTopics(List.of(newTopic));
        createResult.all().get();
    }

    @AfterEach
    void tearDown() {
        if (adminClient != null) {
            adminClient.deleteTopics(List.of(TEST_TOPIC));
            adminClient.deleteAcls(List.of(AclBindingFilter.ANY));
            adminClient.close();
        }
    }

    @Test
    void createAclsForSubject_shouldCreateAclsSuccessfully() throws Exception {
        Result<Void> result = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);

        assertThat(result.succeeded()).isTrue();

        DescribeAclsResult describeResult = adminClient.describeAcls(AclBindingFilter.ANY);
        Collection<AclBinding> aclBindings = describeResult.values().get();

        assertThat(aclBindings).hasSize(3);

        boolean hasTopicReadAcl = aclBindings.stream()
                .anyMatch(acl -> acl.pattern().name().equals(TEST_TOPIC) &&
                        acl.entry().principal().equals("User:" + TEST_OAUTH_SUBJECT) &&
                        acl.entry().operation().equals(AclOperation.READ));

        boolean hasTopicDescribeAcl = aclBindings.stream()
                .anyMatch(acl -> acl.pattern().name().equals(TEST_TOPIC) &&
                        acl.entry().principal().equals("User:" + TEST_OAUTH_SUBJECT) &&
                        acl.entry().operation().equals(AclOperation.DESCRIBE));

        boolean hasGroupReadAcl = aclBindings.stream()
                .anyMatch(acl -> acl.pattern().name().equals(TEST_GROUP_PREFIX) &&
                        acl.entry().principal().equals("User:" + TEST_OAUTH_SUBJECT) &&
                        acl.entry().operation().equals(AclOperation.READ) &&
                        acl.pattern().resourceType().equals(ResourceType.GROUP));

        assertThat(hasTopicReadAcl).isTrue();
        assertThat(hasTopicDescribeAcl).isTrue();
        assertThat(hasGroupReadAcl).isTrue();
    }

    @Test
    void topicAccess_withValidAcls_shouldBeAllowed() throws Exception {
        Result<Void> aclResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);
        assertThat(aclResult.succeeded()).isTrue();

        produceTestMessage(TEST_TOPIC, "test-key", "test-value");

        Properties consumerProps = createConsumerProperties(TEST_OAUTH_SUBJECT);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.assign(List.of(TEST_PARTITION));
            consumer.seekToBeginning(List.of(TEST_PARTITION));

            ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);

            assertThat(records.count()).isEqualTo(1);
            assertThat(records.iterator().next().value()).isEqualTo("test-value");
        }
    }

    @Test
    void topicAccess_withWrongUser_shouldBeBlocked() {
        Result<Void> aclResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);
        assertThat(aclResult.succeeded()).isTrue();

        Properties consumerProps = createConsumerProperties(UNAUTHORIZED_USER);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.assign(List.of(TEST_PARTITION));
            assertThatThrownBy(() -> consumer.poll(POLL_TIMEOUT)).isInstanceOf(TopicAuthorizationException.class);
        }
    }

    @Test
    void revokeAclsForTransferProcess_shouldRemoveAclsSuccessfully() throws Exception {
        Result<Void> createResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);
        assertThat(createResult.succeeded()).isTrue();

        DescribeAclsResult describeResult = adminClient.describeAcls(AclBindingFilter.ANY);
        Collection<AclBinding> aclsBeforeRevoke = describeResult.values().get();

        Result<Void> revokeResult = aclService.revokeAclsForTransferProcess(TEST_TRANSFER_PROCESS_ID);

        assertThat(revokeResult.succeeded()).isTrue();

        DescribeAclsResult describeAfterRevoke = adminClient.describeAcls(AclBindingFilter.ANY);
        Collection<AclBinding> aclsAfterRevoke = describeAfterRevoke.values().get();

        assertThat(aclsBeforeRevoke).hasSize(3);
        assertThat(aclsAfterRevoke).isEmpty();
    }

    @Test
    void revokeAclsForSubject_shouldRemoveAclsSuccessfully() throws Exception {
        Result<Void> createResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);
        assertThat(createResult.succeeded()).isTrue();

        DescribeAclsResult describeResult = adminClient.describeAcls(AclBindingFilter.ANY);
        Collection<AclBinding> aclsBeforeRevoke = describeResult.values().get();

        Result<Void> revokeResult = aclService.revokeAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX);

        assertThat(revokeResult.succeeded()).isTrue();

        DescribeAclsResult describeAfterRevoke = adminClient.describeAcls(AclBindingFilter.ANY);
        Collection<AclBinding> aclsAfterRevoke = describeAfterRevoke.values().get();

        assertThat(aclsBeforeRevoke).hasSize(3);
        assertThat(aclsAfterRevoke).isEmpty();
    }

    @Test
    void topicAccess_afterAclRevocation_shouldBeBlocked() throws Exception {
        Result<Void> createResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);
        assertThat(createResult.succeeded()).isTrue();

        produceTestMessage(TEST_TOPIC, "test-key", "test-value");

        Properties consumerProps = createConsumerProperties(TEST_OAUTH_SUBJECT);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.assign(List.of(TEST_PARTITION));
            consumer.seekToBeginning(List.of(TEST_PARTITION));
            ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
            assertThat(records.count()).isEqualTo(1);
        }

        Result<Void> revokeResult = aclService.revokeAclsForTransferProcess(TEST_TRANSFER_PROCESS_ID);
        assertThat(revokeResult.succeeded()).isTrue();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.assign(List.of(TEST_PARTITION));
            assertThatThrownBy(() -> consumer.poll(POLL_TIMEOUT)).isInstanceOf(TopicAuthorizationException.class);
        }
    }

    @Test
    void revokeAclsForTransferProcess_withNonExistentId_shouldSucceed() {
        Result<Void> result = aclService.revokeAclsForTransferProcess("non-existent-id");

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void multipleTransferProcesses_shouldTrackAclsIndependently() throws Exception {
        String transferProcess1 = "transfer-1";
        String transferProcess2 = "transfer-2";
        String user1 = "user1";
        String user2 = "user2";

        Result<Void> result1 = aclService.createAclsForSubject(user1, TEST_TOPIC, TEST_GROUP_PREFIX, transferProcess1);
        Result<Void> result2 = aclService.createAclsForSubject(user2, TEST_TOPIC, TEST_GROUP_PREFIX, transferProcess2);

        assertThat(result1.succeeded()).isTrue();
        assertThat(result2.succeeded()).isTrue();

        Result<Void> revokeResult = aclService.revokeAclsForTransferProcess(transferProcess1);
        assertThat(revokeResult.succeeded()).isTrue();

        DescribeAclsResult describeResult = adminClient.describeAcls(AclBindingFilter.ANY);
        Collection<AclBinding> remainingAcls = describeResult.values().get();

        boolean user2AclsExist = remainingAcls.stream()
                .anyMatch(acl -> acl.entry().principal().equals("User:" + user2));

        boolean user1AclsExist = remainingAcls.stream()
                .anyMatch(acl -> acl.entry().principal().equals("User:" + user1));

        assertThat(user2AclsExist).isTrue();
        assertThat(user1AclsExist).isFalse();
    }

    @Test
    void consumerGroupJoin_withPrefixedGroup_shouldBeAllowed() throws Exception {
        Result<Void> aclResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);
        assertThat(aclResult.succeeded()).isTrue();

        produceTestMessage(TEST_TOPIC, "test-key", "test-value");

        // group.id starts with the granted prefix. Joining the coordinator via subscribe() requires READ
        // on the GROUP resource, which the fix scopes to the group prefix (not the subject) — so the same
        // prefix handed to the consumer in the EDR is the one the broker authorizes.
        Properties consumerProps = createConsumerProperties(TEST_OAUTH_SUBJECT, TEST_GROUP_PREFIX + "-consumer-1");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of(TEST_TOPIC));

            int count = 0;
            String value = null;
            long deadline = System.currentTimeMillis() + 20_000;
            while (count == 0 && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                count = records.count();
                if (count > 0) {
                    value = records.iterator().next().value();
                }
            }

            assertThat(count).isEqualTo(1);
            assertThat(value).isEqualTo("test-value");
        }
    }

    @Test
    void consumerGroupJoin_withNonPrefixedGroup_shouldBeBlocked() throws Exception {
        Result<Void> aclResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC, TEST_GROUP_PREFIX, TEST_TRANSFER_PROCESS_ID);
        assertThat(aclResult.succeeded()).isTrue();

        produceTestMessage(TEST_TOPIC, "test-key", "test-value");

        // group.id outside the granted prefix: the subject has topic READ/DESCRIBE, but the GROUP READ ACL
        // does not cover this group, so the coordinator join is denied. Guards against widening the ACL.
        Properties consumerProps = createConsumerProperties(TEST_OAUTH_SUBJECT, "other-group");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of(TEST_TOPIC));
            assertThatThrownBy(() -> {
                long deadline = System.currentTimeMillis() + 20_000;
                while (System.currentTimeMillis() < deadline) {
                    consumer.poll(POLL_TIMEOUT);
                }
            }).isInstanceOf(GroupAuthorizationException.class);
        }
    }

    private Properties createConsumerProperties(String username) {
        return createConsumerProperties(username, GROUP_ID);
    }

    private Properties createConsumerProperties(String username, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        props.put(SaslConfigs.SASL_MECHANISM, PlainSaslServer.PLAIN_MECHANISM);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name());
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                ("org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "username=\"%s\" password=\"%s\";").formatted(username, "password"));

        return props;
    }

    private void produceTestMessage(String topic, String key, String value) throws ExecutionException, InterruptedException {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_PLAINTEXT.name());
        producerProps.put(SaslConfigs.SASL_MECHANISM, PlainSaslServer.PLAIN_MECHANISM);
        producerProps.put(SaslConfigs.SASL_JAAS_CONFIG, ADMIN_LOGIN_MODULE);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
            producer.send(producerRecord).get();
            producer.flush();
        }
    }
}
