package com.escola.screi.service;

public class DocumentoInvalidoException extends IllegalArgumentException {
    private final String campo;
    public DocumentoInvalidoException(String campo, String mensagem) { super(mensagem); this.campo=campo; }
    public String getCampo() { return campo; }
}
