package com.escola.screi.dto;

import com.escola.screi.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public class SolicitacaoMatriculaFormDto {
    @Valid @NotNull
    private Aluno aluno = new Aluno();
    @Valid @NotNull
    private Responsavel responsavel = new Responsavel();
    @Valid @NotNull
    private ContatoEmergencia contatoEmergencia = new ContatoEmergencia();
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public Responsavel getResponsavel() { return responsavel; }
    public void setResponsavel(Responsavel responsavel) { this.responsavel = responsavel; }
    public ContatoEmergencia getContatoEmergencia() { return contatoEmergencia; }
    public void setContatoEmergencia(ContatoEmergencia contatoEmergencia) { this.contatoEmergencia = contatoEmergencia; }

}
