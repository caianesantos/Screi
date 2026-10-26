package com.escola.screi.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AzureBlobDocumentoStorage implements DocumentoStorage {
    private final BlobContainerClient container;
    public AzureBlobDocumentoStorage(@Value("${screi.storage.connection-string}") String connectionString,
                                     @Value("${screi.storage.container}") String nomeContainer) {
        container=new BlobServiceClientBuilder().connectionString(connectionString)
            .serviceVersion(BlobServiceVersion.V2023_11_03)
            .retryOptions(new RequestRetryOptions(RetryPolicyType.FIXED, 2, 10, 100L, 500L, null))
            .buildClient().getBlobContainerClient(nomeContainer);
    }
    @Override public void enviar(String chave, byte[] imagem, String contentType) {
        try {
            // Without a public-access option the new container is private.
            container.createIfNotExists();
            container.getBlobClient(chave).uploadWithResponse(new BlobParallelUploadOptions(BinaryData.fromBytes(imagem))
                .setHeaders(new BlobHttpHeaders().setContentType(contentType)), java.time.Duration.ofSeconds(20), com.azure.core.util.Context.NONE);
        } catch (RuntimeException ex) { throw new ArmazenamentoIndisponivelException(ex); }
    }
    @Override public byte[] ler(String chave) {
        try { return container.getBlobClient(chave).downloadContent().toBytes(); }
        catch (RuntimeException ex) { throw new ArmazenamentoIndisponivelException(ex); }
    }
    @Override public void excluir(String chave) {
        try { container.getBlobClient(chave).deleteIfExists(); }
        catch (RuntimeException ex) { throw new ArmazenamentoIndisponivelException(ex); }
    }
}
