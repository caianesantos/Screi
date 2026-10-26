package com.escola.screi.config;

import com.azure.storage.blob.*;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class AzureBlobConfig {
    @Bean public BlobContainerClient blobContainerClient(
            @Value("${screi.storage.connection-string}") String connectionString,
            @Value("${screi.storage.container}") String nomeContainer) {
        return new BlobServiceClientBuilder().connectionString(connectionString)
            .serviceVersion(BlobServiceVersion.V2023_11_03)
            .retryOptions(new RequestRetryOptions(RetryPolicyType.FIXED, 2, 10, 100L, 500L, null))
            .buildClient().getBlobContainerClient(nomeContainer);
    }
}
