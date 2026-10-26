package com.escola.screi.dto;

import com.escola.screi.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import com.escola.screi.model.enums.TipoDocumento;


public class SolicitacaoMatriculaFormDto {
    @Valid @NotNull
    private Aluno aluno = new Aluno();
    @Valid @NotNull
    private Responsavel responsavel = new Responsavel();
    @Valid @NotNull
    private ContatoEmergencia contatoEmergencia = new ContatoEmergencia();
    private MultipartFile cpfAlunoImagem;
    private MultipartFile certidaoNascimentoImagem;
    private MultipartFile cpfResponsavelImagem;
    private MultipartFile comprovanteResidenciaImagem;
    public MultipartFile getCpfAlunoImagem() { return cpfAlunoImagem; }
    public void setCpfAlunoImagem(MultipartFile arquivo) { cpfAlunoImagem=arquivo; }
    public MultipartFile getCertidaoNascimentoImagem() { return certidaoNascimentoImagem; }
    public void setCertidaoNascimentoImagem(MultipartFile arquivo) { certidaoNascimentoImagem=arquivo; }
    public MultipartFile getCpfResponsavelImagem() { return cpfResponsavelImagem; }
    public void setCpfResponsavelImagem(MultipartFile arquivo) { cpfResponsavelImagem=arquivo; }
    public MultipartFile getComprovanteResidenciaImagem() { return comprovanteResidenciaImagem; }
    public void setComprovanteResidenciaImagem(MultipartFile arquivo) { comprovanteResidenciaImagem=arquivo; }
    public MultipartFile arquivo(TipoDocumento tipo) {
        return switch (tipo) {
            case CPF_ALUNO -> cpfAlunoImagem;
            case CERTIDAO_NASCIMENTO -> certidaoNascimentoImagem;
            case CPF_RESPONSAVEL -> cpfResponsavelImagem;
            case COMPROVANTE_RESIDENCIA -> comprovanteResidenciaImagem;
        };
    }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public Responsavel getResponsavel() { return responsavel; }
    public void setResponsavel(Responsavel responsavel) { this.responsavel = responsavel; }
    public ContatoEmergencia getContatoEmergencia() { return contatoEmergencia; }
    public void setContatoEmergencia(ContatoEmergencia contatoEmergencia) { this.contatoEmergencia = contatoEmergencia; }

}
