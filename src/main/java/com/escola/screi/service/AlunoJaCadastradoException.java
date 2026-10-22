package com.escola.screi.service;

public class AlunoJaCadastradoException extends IllegalArgumentException {
    public AlunoJaCadastradoException() {
        super("Já existe um aluno cadastrado com este CPF. Consulte a solicitação existente ou procure a secretaria.");
    }
}
