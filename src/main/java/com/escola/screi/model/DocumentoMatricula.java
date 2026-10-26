package com.escola.screi.model;

import com.escola.screi.model.enums.TipoDocumento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(name="uk_documento_solicitacao_tipo", columnNames={"solicitacao_id","tipo"}))
public class DocumentoMatricula {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="solicitacao_id", nullable=false)
    private SolicitacaoMatricula solicitacao;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private TipoDocumento tipo;
    @Column(nullable=false, unique=true)
    private String chaveBlob;
    @Column(nullable=false)
    private String contentType;
    @Column(nullable=false)
    private long tamanho;
    @Column(nullable=false)
    private LocalDateTime dataEnvio;
    public Long getId() { return id; }
    public SolicitacaoMatricula getSolicitacao() { return solicitacao; }
    public void setSolicitacao(SolicitacaoMatricula solicitacao) { this.solicitacao=solicitacao; }
    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo=tipo; }
    public String getChaveBlob() { return chaveBlob; }
    public void setChaveBlob(String chaveBlob) { this.chaveBlob=chaveBlob; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType=contentType; }
    public long getTamanho() { return tamanho; }
    public void setTamanho(long tamanho) { this.tamanho=tamanho; }
    public LocalDateTime getDataEnvio() { return dataEnvio; }
    @PrePersist void criar() { dataEnvio=LocalDateTime.now(); }
}
