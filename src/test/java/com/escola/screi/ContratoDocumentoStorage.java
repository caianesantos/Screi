package com.escola.screi;

import com.escola.screi.service.DocumentoStorage;
import static org.assertj.core.api.Assertions.*;

/** As mesmas verificações comportamentais são executadas para dois provedores. */
final class ContratoDocumentoStorage {
    private ContratoDocumentoStorage() {}
    static void verificar(DocumentoStorage storage) {
        String chave="contrato/"+java.util.UUID.randomUUID()+".png";
        byte[] enviada=ImagensTeste.bytes("png");
        byte[] esperada=enviada.clone();
        try {
            storage.enviar(chave,enviada,"image/png");
            enviada[0]=0;
            byte[] recebida=storage.ler(chave);
            assertThat(recebida).isEqualTo(esperada);
            recebida[0]=0;
            assertThat(storage.ler(chave)).isEqualTo(esperada);
            storage.excluir(chave);
            assertThatNoException().isThrownBy(() -> storage.excluir(chave));
        } finally { storage.excluir(chave); }
    }
}
