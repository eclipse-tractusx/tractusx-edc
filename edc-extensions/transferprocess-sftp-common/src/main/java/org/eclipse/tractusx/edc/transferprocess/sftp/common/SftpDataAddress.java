/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.common;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Base64;

import static java.lang.String.format;

public class SftpDataAddress extends DataAddress {
    private static final String LOCATION_HOST = "locationHost";
    private static final String LOCATION_PORT = "locationPort";
    private static final String LOCATION_PATH = "locationPath";
    private static final String USER_NAME = "userName";
    private static final String USER_PASSWORD = "userPassword";
    private static final String USER_PRIVATE_KEY = "userPrivateKey";


    private static final String CONNECTION_TYPE = "sftp";

    private SftpDataAddress() {

    }

    public static SftpDataAddress fromDataAddress(DataAddress dataAddress) throws EdcSftpException {
        if (dataAddress instanceof SftpDataAddress) {
            return (SftpDataAddress) dataAddress;
        }

        if (!dataAddress.getType().equalsIgnoreCase("sftp")) {
            throw new EdcSftpException(format("Invalid DataAddress type: %s. Expected %s.",
                    dataAddress.getType(), CONNECTION_TYPE));
        }

        try {
            var sftpUser = createSftpUser(dataAddress);

            var sftpLocation = createSftpLocation(dataAddress);

            return SftpDataAddress.Builder.newInstance()
                    .sftpUser(sftpUser)
                    .sftpLocation(sftpLocation)
                    .build();
        } catch (NullPointerException e) {
            throw new EdcSftpException(e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new EdcSftpException(format("Port for SftpLocation %s/%s not a number", dataAddress.getProperty(LOCATION_HOST), dataAddress.getProperty(LOCATION_PATH)), e);
        }
    }

    private static SftpLocation createSftpLocation(DataAddress dataAddress) {
        return SftpLocation.Builder.newInstance()
                .host(dataAddress.getProperty(LOCATION_HOST))
                .port(Integer.parseInt(dataAddress.getProperty(LOCATION_PORT, "22")))
                .path(dataAddress.getProperty(LOCATION_PATH))
                .build();
    }

    private static SftpUser createSftpUser(DataAddress dataAddress) {
        return SftpUser.Builder.newInstance()
                .name(dataAddress.getProperty(USER_NAME))
                .password(dataAddress.getProperty(USER_PASSWORD))
                .keyPair(SftpUserKeyPairGenerator.getKeyPairFromPrivateKey(
                        dataAddress.getProperty(USER_PRIVATE_KEY),
                        dataAddress.getProperty(USER_NAME)))
                .build();
    }

    public SftpUser getSftpUser() {
        return createSftpUser(this);
    }

    public SftpLocation getSftpLocation() {
        return createSftpLocation(this);
    }

    public static class Builder extends DataAddress.Builder<SftpDataAddress, Builder> {
        protected Builder(SftpDataAddress address) {
            super(address);
        }

        public static Builder newInstance() {
            return new Builder(new SftpDataAddress());
        }

        public Builder sftpUser(SftpUser user) {
            this.address.getProperties().put(USER_NAME, user.getName());
            this.address.getProperties().put(USER_PASSWORD, user.getPassword());
            if (user.getKeyPair() != null) {
                this.address.getProperties().put(USER_PRIVATE_KEY, Base64.getEncoder().encodeToString(user.getKeyPair().getPrivate().getEncoded()));
                this.address.getProperties().put(EDC_DATA_ADDRESS_KEY_NAME, user.getName());
            }
            return this;
        }

        public Builder sftpLocation(SftpLocation location) {
            this.address.getProperties().put(LOCATION_HOST, location.getHost());
            this.address.getProperties().put(LOCATION_PORT, String.valueOf(location.getPort()));
            this.address.getProperties().put(LOCATION_PATH, location.getPath());
            return this;
        }

        @Override
        public SftpDataAddress build() {
            address.setType(CONNECTION_TYPE);

            super.build(); //for validation
            return address;
        }
    }

}
