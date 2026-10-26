package com.escola.screi.model;

import jakarta.persistence.*;
import com.escola.screi.model.enums.StatusMatricula;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class SolicitacaoMatricula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, updatable=false)
    private String protocolo;
    @OneToOne(cascade=CascadeType.ALL, optional=false, orphanRemoval=true)
    private Aluno aluno;
    @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true) @JoinColumn(name="solicitacao_id")
    private List<Responsavel> responsaveis = new ArrayList<>();
    @OneToMany(cascade=CascadeType.ALL, orphanRemoval=true) @JoinColumn(name="solicitacao_id")
    private List<ContatoEmergencia> contatosEmergencia = new ArrayList<>();
    @OneToMany(mappedBy="solicitacao", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<DocumentoMatricula> documentos = new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private StatusMatricula status = StatusMatricula.ENVIADO;
    @Column(length=2000)
    private String justificativaReprovacao;
    @Column(nullable=false, updatable=false)
    private LocalDateTime dataSubmissao;
    @Column(nullable=false)
    private LocalDateTime dataAtualizacao;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public List<Responsavel> getResponsaveis() { return responsaveis; }
    public void setResponsaveis(List<Responsavel> responsaveis) { this.responsaveis = responsaveis; }
    public List<ContatoEmergencia> getContatosEmergencia() { return contatosEmergencia; }
    public void setContatosEmergencia(List<ContatoEmergencia> contatosEmergencia) { this.contatosEmergencia = contatosEmergencia; }
    public List<DocumentoMatricula> getDocumentos() { return documentos; }
    public StatusMatricula getStatus() { return status; }
    public void setStatus(StatusMatricula status) { this.status = status; }
    public String getJustificativaReprovacao() { return justificativaReprovacao; }
    public void setJustificativaReprovacao(String justificativaReprovacao) { this.justificativaReprovacao = justificativaReprovacao; }
    public LocalDateTime getDataSubmissao() { return dataSubmissao; }
    public void setDataSubmissao(LocalDateTime dataSubmissao) { this.dataSubmissao = dataSubmissao; }
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

    @PrePersist
    void criar() {
        if (protocolo == null) protocolo = java.util.UUID.randomUUID().toString();
        dataSubmissao = LocalDateTime.now();
        dataAtualizacao = dataSubmissao;
        validarPendencia();
    }
    @PreUpdate
    void atualizar() { validarPendencia(); dataAtualizacao = LocalDateTime.now(); }
    private void validarPendencia() {
        if (status == StatusMatricula.COM_PENDENCIA && (justificativaReprovacao == null || justificativaReprovacao.isBlank()))
            throw new IllegalArgumentException("Informe a justificativa da pendência.");
    }

}
