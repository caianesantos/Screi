package com.escola.screi;

import com.escola.screi.service.DocumentoStorage;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

class DocumentoStorageContratoTests {
    @Test void armazenamentoEmMemoriaPreservaOContratoDoProvedor() {
        ContratoDocumentoStorage.verificar(new StorageEmMemoria());
    }
    private static class StorageEmMemoria implements DocumentoStorage {
        private final Map<String, Objeto> objetos=new HashMap<>();
        @Override public void enviar(String chave, byte[] imagem, String contentType) {
            objetos.put(chave,new Objeto(imagem.clone(),contentType));
        }
        @Override public byte[] ler(String chave) {
            var objeto=objetos.get(chave);
            if (objeto==null) throw new IllegalArgumentException("Objeto não encontrado.");
            return objeto.dados().clone();
        }
        @Override public void excluir(String chave) { objetos.remove(chave); }
        private record Objeto(byte[] dados, String contentType) {}
    }
}
