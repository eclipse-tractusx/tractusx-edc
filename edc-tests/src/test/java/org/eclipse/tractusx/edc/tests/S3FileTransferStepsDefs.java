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

package org.eclipse.tractusx.edc.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.AfterAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.tractusx.edc.tests.data.Asset;
import org.eclipse.tractusx.edc.tests.data.DataAddress;
import org.eclipse.tractusx.edc.tests.data.Negotiation;
import org.eclipse.tractusx.edc.tests.data.Permission;
import org.eclipse.tractusx.edc.tests.data.Policy;
import org.eclipse.tractusx.edc.tests.data.S3DataAddress;
import org.eclipse.tractusx.edc.tests.data.Transfer;
import org.eclipse.tractusx.edc.tests.util.S3Client;
import org.eclipse.tractusx.edc.tests.util.Timeouts;
import org.junit.jupiter.api.Assertions;

public class S3FileTransferStepsDefs {

  @Given("'{connector}' has an empty storage bucket called {string}")
  public void hasEmptyStorageBucket(Connector connector, String bucketName) {
    S3Client s3 = connector.getS3Client();

    s3.createBucket(bucketName);

    Assertions.assertTrue(s3.listBuckets().contains(bucketName));
    Assertions.assertEquals(0, s3.listBucketContent(bucketName).size());
  }

  private File fileToTransfer;

  @Given("'{connector}' has a storage bucket called {string} with the file called {string}")
  public void hasAStorageBucketWithTheFile(Connector connector, String bucketName, String fileName)
      throws IOException {

    S3Client s3 = connector.getS3Client();
    s3.createBucket(bucketName);
    fileToTransfer = s3.uploadFile(bucketName, fileName);

    Set<String> bucketContent = s3.listBucketContent(bucketName);

    Assertions.assertEquals(1, bucketContent.size());
    Assertions.assertTrue(bucketContent.contains(fileName));
  }

  @Given("'{connector}' has the following S3 assets")
  public void hasAssets(Connector connector, DataTable table) {
    final DataManagementAPI api = connector.getDataManagementAPI();

    parseDataTable(table)
        .forEach(
            asset -> {
              try {
                api.createAsset(asset);
              } catch (IOException e) {
                fail(e.getMessage());
              }
            });
  }

  private String assetId;
  private String agreementId;

  @Then("'{connector}' negotiates the contract successfully with '{connector}'")
  public void negotiateContract(Connector sender, Connector receiver, DataTable dataTable)
      throws IOException {

    String definitionId = dataTable.asMaps().get(0).get("contract offer id");
    assetId = dataTable.asMaps().get(0).get("asset id");
    String policyId = dataTable.asMaps().get(0).get("policy id");

    final Policy policy =
        new Policy(policyId, List.of(new Permission("USE", null, new ArrayList<>())));

    final DataManagementAPI dataManagementAPI = sender.getDataManagementAPI();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    final Negotiation negotiation =
        dataManagementAPI.initiateNegotiation(receiverIdsUrl, definitionId, assetId, policy);
    negotiation.waitUntilComplete(dataManagementAPI);

    agreementId = dataManagementAPI.getNegotiation(negotiation.getId()).getAgreementId();
  }

  @Then("'{connector}' initiate S3 transfer process from '{connector}'")
  public void initiateTransferProcess(Connector sender, Connector receiver, DataTable dataTable)
      throws IOException {
    DataAddress dataAddress = createDataAddress(dataTable.asMaps().get(0));

    final DataManagementAPI dataManagementAPI = sender.getDataManagementAPI();
    final String receiverIdsUrl = receiver.getEnvironment().getIdsUrl() + "/data";

    final Transfer transferProcess =
        dataManagementAPI.initiateTransferProcess(
            receiverIdsUrl, agreementId, assetId, dataAddress);
    transferProcess.waitUntilComplete(dataManagementAPI);

    Assertions.assertNotNull(transferProcess.getId());
  }

  private static final String COMPLETION_MARKER = ".complete";

  @Then("'{connector}' has a storage bucket called {string} with transferred file called {string}")
  public void consumerHasAStorageBucketWithFileTransferred(
      Connector connector, String bucketName, String fileName) throws IOException {
    S3Client s3 = connector.getS3Client();
    await()
        .pollDelay(Duration.ofMillis(500))
        .atMost(Timeouts.FILE_TRANSFER)
        .until(() -> isFilePresent(s3, bucketName, fileName + COMPLETION_MARKER));

    Set<String> bucketContent = s3.listBucketContent(bucketName);

    Assertions.assertEquals(2, bucketContent.size());
    Assertions.assertTrue(bucketContent.contains(fileName));
    Assertions.assertArrayEquals(
        Files.readAllBytes(fileToTransfer.toPath()),
        Files.readAllBytes(s3.downloadFile(bucketName, fileName).toPath()));
  }

  private boolean isFilePresent(S3Client s3, String bucketName, String fileName) {
    return s3.listBucketContent(bucketName).contains(fileName);
  }

  private List<Asset> parseDataTable(DataTable table) {
    final List<Asset> assetsWithDataAddresses = new ArrayList<>();

    for (Map<String, String> map : table.asMaps()) {
      String id = map.get("id");
      String description = map.get("description");
      assetsWithDataAddresses.add(new Asset(id, description, createDataAddress(map)));
    }

    return assetsWithDataAddresses;
  }

  private DataAddress createDataAddress(Map<String, String> map) {
    final String bucketName = map.get("data_address_s3_bucket_name");
    final String region = map.get("data_address_s3_region");
    final String keyName = map.get("data_address_s3_key_name");
    return new S3DataAddress(bucketName, region, keyName);
  }

  @AfterAll
  public static void bucketsCleanup() {
    S3Client s3 = new S3Client(Environment.byName("Sokrates"));
    s3.deleteAllBuckets();
  }
}
