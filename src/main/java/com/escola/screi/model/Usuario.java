package com.escola.screi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
import com.escola.screi.model.enums.TipoPapel;
@Entity @Table(name="usuarios")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    private String nome;
    @NotBlank @Email @Column(unique=true, nullable=false)
    private String email;
    @NotBlank
    private String senha;
    @NotNull @Enumerated(EnumType.STRING)
    private TipoPapel papel;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public TipoPapel getPapel() { return papel; }
    public void setPapel(TipoPapel papel) { this.papel = papel; }

}
