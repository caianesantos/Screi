package com.escola.screi.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.springframework.stereotype.Service;

@Service
public class AzureBlobDocumentoStorage implements DocumentoStorage {
    private final BlobContainerClient container;
    public AzureBlobDocumentoStorage(BlobContainerClient container) { this.container=container; }
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
