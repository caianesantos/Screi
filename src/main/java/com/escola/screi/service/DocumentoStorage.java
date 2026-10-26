package com.escola.screi.service;

/**
 * Armazenamento binário independente de provedor.
 * As operações são síncronas; falhas são sinalizadas por RuntimeException.
 * enviar preserva os bytes recebidos; leituras não expõem memória mutável do armazenamento.
 * excluir é idempotente: uma chave já ausente não constitui erro.
 */
public interface DocumentoStorage {
    void enviar(String chave, byte[] imagem, String contentType);
    byte[] ler(String chave);
    void excluir(String chave);
}
