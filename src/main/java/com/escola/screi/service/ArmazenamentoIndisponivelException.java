package com.escola.screi.service;

public class ArmazenamentoIndisponivelException extends RuntimeException {
    public ArmazenamentoIndisponivelException(Throwable causa) {
        super("Armazenamento de documentos temporariamente indisponível. Tente novamente.", causa);
    }
}
