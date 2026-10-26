package com.escola.screi.service;

public interface DocumentoStorage {
    void enviar(String chave, byte[] imagem, String contentType);
    byte[] ler(String chave);
    void excluir(String chave);
}
