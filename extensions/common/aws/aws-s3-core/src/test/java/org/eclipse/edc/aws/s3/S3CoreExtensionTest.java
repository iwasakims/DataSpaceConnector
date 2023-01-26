/*
 *  Copyright (c) 2023 NTT DATA Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Masatake Iwasaki (NTT DATA) - Initial implementation
 *
 */

package org.eclipse.edc.aws.s3;

import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.junit.testfixtures.MockVault;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
public class S3CoreExtensionTest {
    private static final String VAULT_KEY_ACCESS_KEY_ID = "key of access key id";
    private static final String VAULT_KEY_SECRET_ACCESS_KEY = "key of secret access key";
    private static final String EDC_AWS_ACCESS_KEY_ID = "access key id in vault";
    private static final String EDC_AWS_SECRET_ACCESS_KEY = "secret access key in vault";
    private static final String AWS_ACCESS_KEY_ID = "access key id via AWS SDK settings";
    private static final String AWS_SECRET_ACCESS_KEY = "secret access key via AWS SDK settings";

    private Properties savedProperties;
    private S3CoreExtension s3;

    @BeforeEach
    void setUp(EdcExtension edc) {
        savedProperties = (Properties) System.getProperties().clone();

        // AWS SDK settings via system properties
        System.setProperty("aws.accessKeyId", AWS_ACCESS_KEY_ID);
        System.setProperty("aws.secretAccessKey", AWS_SECRET_ACCESS_KEY);

        edc.setConfiguration(Map.of(
                S3CoreExtension.AWS_ACCESS_KEY, VAULT_KEY_ACCESS_KEY_ID,
                S3CoreExtension.AWS_SECRET_KEY, VAULT_KEY_SECRET_ACCESS_KEY));
        edc.registerServiceMock(Vault.class, new MockVault());
        s3 = new S3CoreExtension();
        edc.registerSystemExtension(ServiceExtension.class, s3);
    }

    @AfterEach
    void afterEach() {
        System.setProperties(savedProperties);
    }

    @Test
    void testCredentialsFromVault(EdcExtension edc, Vault vault) {
        vault.storeSecret(VAULT_KEY_ACCESS_KEY_ID, EDC_AWS_ACCESS_KEY_ID);
        vault.storeSecret(VAULT_KEY_SECRET_ACCESS_KEY, EDC_AWS_SECRET_ACCESS_KEY);

        // EDC configurations have higher precedence than AWS SDK settings
        var credentials = s3.createCredentialsProvider(edc.getContext()).resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo(EDC_AWS_ACCESS_KEY_ID);
        assertThat(credentials.secretAccessKey()).isEqualTo(EDC_AWS_SECRET_ACCESS_KEY);
    }

    @Test
    void testCredentialsFromAwsSdk(EdcExtension edc) {
        var credentials = s3.createCredentialsProvider(edc.getContext()).resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo(AWS_ACCESS_KEY_ID);
        assertThat(credentials.secretAccessKey()).isEqualTo(AWS_SECRET_ACCESS_KEY);
    }

    @Test
    void testAwsClientProviderCaching(EdcExtension edc) {
        var provider = s3.awsClientProvider(edc.getContext());
        var client1 = provider.s3Client("us-east-1");
        var client2 = provider.s3Client("us-west-1");
        var client3 = provider.s3Client("us-west-1");
        assertThat(client3).isSameAs(client2).isNotSameAs(client1);

        var asyncClient1 = provider.s3AsyncClient("us-east-1");
        var asyncClient2 = provider.s3AsyncClient("us-west-1");
        var asyncClient3 = provider.s3AsyncClient("us-west-1");
        assertThat(client3).isSameAs(client2).isNotSameAs(client1);
    }
}
