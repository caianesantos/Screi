package com.escola.screi.service;

import com.escola.screi.model.SolicitacaoMatricula;

/** Persiste a solicitação e sinaliza CPF duplicado com a exceção de negócio. */
public interface RegistroSolicitacao {
    SolicitacaoMatricula salvar(SolicitacaoMatricula solicitacao);
}
