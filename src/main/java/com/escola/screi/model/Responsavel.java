package com.escola.screi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
@Entity
public class Responsavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max=200)
    private String nomeCompleto;
    @NotBlank @Size(max=100)
    private String parentesco;
    @NotBlank @CPF
    private String cpf;
    @NotBlank @Pattern(regexp="[0-9()+ .-]{10,20}")
    private String telefone;
    @NotBlank @Email @Size(max=254)
    private String email;
    @NotBlank @Pattern(regexp="[0-9]{8}")
    private String cep;
    @NotBlank @Size(max=200)
    private String logradouro;
    @NotBlank @Size(max=20)
    private String numero;
    @NotBlank @Size(max=100)
    private String bairro;
    @NotBlank @Size(max=100)
    private String cidade;
    @NotBlank @Pattern(regexp="[A-Z]{2}")
    private String estado;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

}
