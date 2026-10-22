package com.escola.screi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name="uk_aluno_cpf", columnNames="cpf"))
public class Aluno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max=200)
    private String nomeCompleto;
    @NotNull @PastOrPresent
    @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate dataNascimento;
    @NotBlank @Size(max=100)
    private String certidaoNascimento;
    @NotBlank @CPF
    @Column(nullable=false, length=11)
    private String cpf;
    @NotBlank @Size(max=2000) @Column(length=2000)
    private String autorizadosBuscar;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public java.time.LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(java.time.LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getCertidaoNascimento() { return certidaoNascimento; }
    public void setCertidaoNascimento(String certidaoNascimento) { this.certidaoNascimento = certidaoNascimento; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getAutorizadosBuscar() { return autorizadosBuscar; }
    public void setAutorizadosBuscar(String autorizadosBuscar) { this.autorizadosBuscar = autorizadosBuscar; }

    @PrePersist @PreUpdate
    void normalizarCpf() {
        if (cpf != null) cpf = cpf.replaceAll("[.\\s-]", "");
    }
}
