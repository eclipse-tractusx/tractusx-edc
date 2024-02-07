/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.dataplane.transfer.test;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.List;

public class TestFunctions {
    public static File createSparseFile(long sizeBytes) {
        try {
            var name = Files.createTempFile("", ".bin");
            try (var f = new RandomAccessFile(name.toFile(), "rw")) {
                f.setLength(sizeBytes);
                f.write("foobar".getBytes());
            }
            return name.toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<String> listObjects(S3Client consumerClient, String bucketName) {
        var response = consumerClient.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
        return response.contents().stream().map(S3Object::key).toList();
    }
}
