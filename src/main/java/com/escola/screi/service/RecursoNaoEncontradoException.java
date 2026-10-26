package com.escola.screi.service;

/** Ausência de recurso; a camada web decide como representar esse resultado em HTTP. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) { super(mensagem); }
}
