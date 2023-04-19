/* Copyright (c) 2022 ZF Friedrichshafen AG
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.tests.util;

import org.eclipse.tractusx.edc.tests.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class S3Client {
    private static final Logger log = LoggerFactory.getLogger(S3Client.class);
    private final software.amazon.awssdk.services.s3.S3Client s3;

    public S3Client(Environment environment) {

        s3 =
                software.amazon.awssdk.services.s3.S3Client.builder()
                        .region(Region.US_EAST_1)
                        .forcePathStyle(true)
                        .endpointOverride(URI.create(environment.getAwsEndpointOverride()))
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(
                                                environment.getAwsAccessKey(), environment.getAwsSecretAccessKey())))
                        .build();
    }

    public void createBucket(String bucketName) {
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("'{}' bucket already owned - skipped bucket creation", bucketName);
        }
    }

    public File uploadFile(String bucketName, String fileName) throws IOException {
        File tempFile = File.createTempFile(fileName, null);
        Files.write(
                tempFile.toPath(), "Will fail if the file has no content".getBytes(StandardCharsets.UTF_8));

        s3.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(fileName).build(),
                RequestBody.fromFile(tempFile));

        return tempFile;
    }

    public List<String> listBuckets() {
        return s3.listBuckets().buckets().stream().map(Bucket::name).collect(Collectors.toList());
    }

    public Set<String> listBucketContent(String bucketName) {
        return s3
                .listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
                .contents()
                .stream()
                .map(S3Object::key)
                .collect(Collectors.toSet());
    }

    public File downloadFile(String bucketName, String fileName) throws IOException {
        ResponseBytes<GetObjectResponse> objectAsBytes =
                s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(fileName).build());

        return Files.write(File.createTempFile(fileName, null).toPath(), objectAsBytes.asByteArray())
                .toFile();
    }

    public void deleteAllBuckets() {
        List<Bucket> buckets = s3.listBuckets().buckets();
        buckets.forEach(this::clearBucket);
        buckets.forEach(
                bucket -> s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket.name()).build()));
    }

    private void clearBucket(Bucket bucket) {
        String bucketName = bucket.name();
        s3.listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
                .contents()
                .forEach(
                        s3Object ->
                                s3.deleteObject(
                                        DeleteObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build()));
    }
}
