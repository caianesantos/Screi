# SCREI — código-fonte integral

Cada seção contém um arquivo completo do projeto, sem omissões. Consulte `README.md` para execução, credenciais e configuração.

## `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>3.5.0</version><relativePath/></parent>
  <groupId>com.escola</groupId><artifactId>screi</artifactId><version>1.0.0</version><name>SCREI</name>
  <properties><java.version>17</java.version></properties>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.apache.poi</groupId><artifactId>poi-ooxml</artifactId><version>5.4.1</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
```

## `.mvn/wrapper/maven-wrapper.properties`

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
```

## `src/main/java/com/escola/screi/config/DataInitializer.java`

```java
package com.escola.screi.config;

import com.escola.screi.model.Usuario;
import com.escola.screi.model.enums.TipoPapel;
import com.escola.screi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DataInitializer implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;
    private final String email;
    private final String senha;
    public DataInitializer(UsuarioRepository usuarios, BCryptPasswordEncoder encoder,
        @Value("${screi.secretaria.email}") String email, @Value("${screi.secretaria.senha}") String senha) {
        this.usuarios=usuarios; this.encoder=encoder; this.email=email; this.senha=senha;
    }
    @Override public void run(String... args) {
        if (usuarios.findByEmailIgnoreCase(email).isEmpty()) {
            Usuario usuario=new Usuario(); usuario.setNome("Secretaria escolar"); usuario.setEmail(email);
            usuario.setSenha(encoder.encode(senha)); usuario.setPapel(TipoPapel.ROLE_SECRETARIA); usuarios.save(usuario);
        }
    }
}
```

## `src/main/java/com/escola/screi/config/SecurityConfig.java`

```java
package com.escola.screi.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/responsavel/**", "/css/**", "/js/**", "/error").permitAll()
            .requestMatchers("/secretaria/**").hasRole("SECRETARIA")
            .anyRequest().authenticated())
            .formLogin(login -> login.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/secretaria/triagem", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .build();
    }
}
```

## `src/main/java/com/escola/screi/controller/AuthController.java`

```java
package com.escola.screi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AuthController {
    @GetMapping("/") public String inicio() { return "redirect:/responsavel/novo"; }
    @GetMapping("/login") public String login() { return "login"; }
}
```

## `src/main/java/com/escola/screi/controller/ResponsavelController.java`

```java
package com.escola.screi.controller;

import com.escola.screi.dto.SolicitacaoMatriculaFormDto;
import com.escola.screi.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
@Controller @RequestMapping("/responsavel")
public class ResponsavelController {
    private final SolicitacaoService solicitacoes;
    private final ViaCepService viaCep;
    public ResponsavelController(SolicitacaoService solicitacoes, ViaCepService viaCep) { this.solicitacoes=solicitacoes; this.viaCep=viaCep; }
    @InitBinder("form") void vincular(WebDataBinder binder) {
        binder.setAllowedFields("aluno.nomeCompleto","aluno.dataNascimento","aluno.certidaoNascimento","aluno.cpf","aluno.autorizadosBuscar",
            "responsavel.nomeCompleto","responsavel.parentesco","responsavel.cpf","responsavel.telefone","responsavel.email","responsavel.cep",
            "responsavel.logradouro","responsavel.numero","responsavel.bairro","responsavel.cidade","responsavel.estado",
            "contatoEmergencia.nome","contatoEmergencia.parentesco","contatoEmergencia.telefone");
    }
    @GetMapping("/novo") public String novo(Model model) { model.addAttribute("form",new SolicitacaoMatriculaFormDto()); return "wizard"; }
    @PostMapping("/solicitacoes") public String enviar(@Valid @ModelAttribute("form") SolicitacaoMatriculaFormDto form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) return "wizard";
        try {
            var s=solicitacoes.enviar(form);
            flash.addFlashAttribute("sucesso","Solicitação enviada. Guarde seu protocolo: "+s.getProtocolo());
            return "redirect:/responsavel/status?consulta="+s.getProtocolo();
        } catch (AlunoJaCadastradoException ex) {
            result.rejectValue("aluno.cpf", "aluno.cpf.duplicado", ex.getMessage());
            return "wizard";
        }
    }
    @GetMapping("/status") public String status(@RequestParam(defaultValue="") String consulta, Model model) {
        model.addAttribute("consulta",consulta); model.addAttribute("solicitacoes",solicitacoes.consultar(consulta)); return "status";
    }
    @GetMapping("/cep/{cep}") @ResponseBody public ResponseEntity<?> cep(@PathVariable String cep) {
        try { return ResponseEntity.ok(viaCep.consultar(cep)); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("mensagem",ex.getMessage())); }
        catch (RestClientException ex) { return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(java.util.Map.of("mensagem","Consulta indisponível. Preencha o endereço manualmente.")); }
    }
}
```

## `src/main/java/com/escola/screi/controller/SecretariaController.java`

```java
package com.escola.screi.controller;

import com.escola.screi.dto.StatusUpdateDto;
import com.escola.screi.model.enums.StatusMatricula;
import com.escola.screi.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import java.io.IOException;
@Controller @RequestMapping("/secretaria")
public class SecretariaController {
    private final SolicitacaoService solicitacoes;
    private final ExcelExportService excel;
    public SecretariaController(SolicitacaoService solicitacoes, ExcelExportService excel) { this.solicitacoes=solicitacoes; this.excel=excel; }
    @GetMapping({"", "/triagem"}) public String triagem(Model model) { model.addAttribute("solicitacoes",solicitacoes.listar()); return "triagem"; }
    @GetMapping("/solicitacoes/{id}") public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("solicitacao",solicitacoes.detalhar(id)); model.addAttribute("statusDisponiveis",StatusMatricula.values());
        if (!model.containsAttribute("atualizacao")) model.addAttribute("atualizacao",new StatusUpdateDto()); return "detalhes";
    }
    @PostMapping("/solicitacoes/{id}/status") public String atualizar(@PathVariable Long id,
        @Valid @ModelAttribute("atualizacao") StatusUpdateDto dto, BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) return detalhes(id,model);
        try { solicitacoes.atualizarStatus(id,dto); }
        catch (IllegalArgumentException ex) { result.reject("status",ex.getMessage()); return detalhes(id,model); }
        flash.addFlashAttribute("sucesso","Situação atualizada."); return "redirect:/secretaria/solicitacoes/"+id;
    }
    @GetMapping("/exportar-excel") public ResponseEntity<byte[]> exportar() throws IOException {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=matriculas-aprovadas.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excel.exportar());
    }
}
```

## `src/main/java/com/escola/screi/dto/SolicitacaoMatriculaFormDto.java`

```java
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
```

## `src/main/java/com/escola/screi/dto/StatusUpdateDto.java`

```java
package com.escola.screi.dto;

import com.escola.screi.model.enums.StatusMatricula;
import jakarta.validation.constraints.*;


public class StatusUpdateDto {
    @NotNull
    private StatusMatricula status;
    @Size(max=2000)
    private String justificativaReprovacao;
    public StatusMatricula getStatus() { return status; }
    public void setStatus(StatusMatricula status) { this.status = status; }
    public String getJustificativaReprovacao() { return justificativaReprovacao; }
    public void setJustificativaReprovacao(String justificativaReprovacao) { this.justificativaReprovacao = justificativaReprovacao; }

    @AssertTrue(message="Informe a justificativa para registrar uma pendência.")
    public boolean isJustificativaValida() {
        return status != StatusMatricula.COM_PENDENCIA ||
            (justificativaReprovacao != null && !justificativaReprovacao.isBlank());
    }

}
```

## `src/main/java/com/escola/screi/model/Aluno.java`

```java
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
```

## `src/main/java/com/escola/screi/model/ContatoEmergencia.java`

```java
package com.escola.screi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
@Entity
public class ContatoEmergencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max=200)
    private String nome;
    @NotBlank @Size(max=100)
    private String parentesco;
    @NotBlank @Pattern(regexp="[0-9()+ .-]{10,20}")
    private String telefone;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

}
```

## `src/main/java/com/escola/screi/model/enums/StatusMatricula.java`

```java
package com.escola.screi.model.enums;

public enum StatusMatricula { ENVIADO, EM_ANALISE, APROVADO, COM_PENDENCIA }
```

## `src/main/java/com/escola/screi/model/enums/TipoPapel.java`

```java
package com.escola.screi.model.enums;

public enum TipoPapel { ROLE_RESPONSAVEL, ROLE_SECRETARIA }
```

## `src/main/java/com/escola/screi/model/Responsavel.java`

```java
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
```

## `src/main/java/com/escola/screi/model/SolicitacaoMatricula.java`

```java
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
```

## `src/main/java/com/escola/screi/model/Usuario.java`

```java
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
```

## `src/main/java/com/escola/screi/repository/AlunoRepository.java`

```java
package com.escola.screi.repository;

import com.escola.screi.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
    boolean existsByCpf(String cpf);
}
```

## `src/main/java/com/escola/screi/repository/SolicitacaoMatriculaRepository.java`

```java
package com.escola.screi.repository;

import com.escola.screi.model.SolicitacaoMatricula;
import com.escola.screi.model.enums.StatusMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SolicitacaoMatriculaRepository extends JpaRepository<SolicitacaoMatricula, Long> {
    List<SolicitacaoMatricula> findByProtocoloIgnoreCase(String protocolo);
    List<SolicitacaoMatricula> findDistinctByResponsaveisCpfOrderByDataSubmissaoDesc(String cpf);
    List<SolicitacaoMatricula> findByStatusOrderByDataSubmissaoDesc(StatusMatricula status);
    List<SolicitacaoMatricula> findAllByOrderByDataSubmissaoDesc();
}
```

## `src/main/java/com/escola/screi/repository/UsuarioRepository.java`

```java
package com.escola.screi.repository;

import com.escola.screi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
```

## `src/main/java/com/escola/screi/ScreiApplication.java`

```java
package com.escola.screi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class ScreiApplication {
    public static void main(String[] args) { SpringApplication.run(ScreiApplication.class, args); }
}
```

## `src/main/java/com/escola/screi/service/AlunoJaCadastradoException.java`

```java
package com.escola.screi.service;

public class AlunoJaCadastradoException extends IllegalArgumentException {
    public AlunoJaCadastradoException() {
        super("Já existe um aluno cadastrado com este CPF. Consulte a solicitação existente ou procure a secretaria.");
    }
}
```

## `src/main/java/com/escola/screi/service/CustomUserDetailsService.java`

```java
package com.escola.screi.service;

import com.escola.screi.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public CustomUserDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    @Override public UserDetails loadUserByUsername(String email) {
        var usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
        return User.withUsername(usuario.getEmail()).password(usuario.getSenha()).authorities(usuario.getPapel().name()).build();
    }
}
```

## `src/main/java/com/escola/screi/service/ExcelExportService.java`

```java
package com.escola.screi.service;

import com.escola.screi.repository.SolicitacaoMatriculaRepository;
import com.escola.screi.model.enums.StatusMatricula;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
@Service
public class ExcelExportService {
    private final SolicitacaoMatriculaRepository repository;
    public ExcelExportService(SolicitacaoMatriculaRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true)
    public byte[] exportar() throws IOException {
        try (var workbook=new XSSFWorkbook(); var output=new ByteArrayOutputStream()) {
            Sheet sheet=workbook.createSheet("Matrículas aprovadas");
            String[] headers={"Protocolo","Aluno","Nascimento","Certidão","CPF do aluno","Autorizados a buscar","Responsáveis","CPF dos responsáveis","Telefones","E-mails","Endereços","Contatos de emergência","Status","Submissão","Atualização"};
            Font font=workbook.createFont(); font.setBold(true);
            CellStyle style=workbook.createCellStyle(); style.setFont(font);
            Row header=sheet.createRow(0);
            for (int i=0;i<headers.length;i++) { header.createCell(i).setCellValue(headers[i]); header.getCell(i).setCellStyle(style); }
            int index=1;
            for (var s:repository.findByStatusOrderByDataSubmissaoDesc(StatusMatricula.APROVADO)) {
                var a=s.getAluno(); var rs=s.getResponsaveis();
                String[] values={s.getProtocolo(),a.getNomeCompleto(),a.getDataNascimento().toString(),a.getCertidaoNascimento(),a.getCpf(),a.getAutorizadosBuscar(),
                    rs.stream().map(r -> r.getNomeCompleto()+" ("+r.getParentesco()+")").collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getCpf()).collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getTelefone()).collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getEmail()).collect(Collectors.joining("; ")),
                    rs.stream().map(r -> r.getLogradouro()+", "+r.getNumero()+", "+r.getBairro()+", "+r.getCidade()+"/"+r.getEstado()+" CEP "+r.getCep()).collect(Collectors.joining("; ")),
                    s.getContatosEmergencia().stream().map(c -> c.getNome()+" ("+c.getParentesco()+") "+c.getTelefone()).collect(Collectors.joining("; ")),
                    s.getStatus().name(),s.getDataSubmissao().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),s.getDataAtualizacao().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)};
                Row row=sheet.createRow(index++);
                for (int i=0;i<values.length;i++) row.createCell(i,CellType.STRING).setCellValue(values[i]);
            }
            sheet.createFreezePane(0,1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,index-1),0,headers.length-1));
            for (int i=0;i<headers.length;i++) sheet.setColumnWidth(i, i==0 ? 10000 : 6500);
            workbook.write(output); return output.toByteArray();
        }
    }
}
```

## `src/main/java/com/escola/screi/service/SolicitacaoService.java`

```java
package com.escola.screi.service;

import com.escola.screi.dto.*;
import com.escola.screi.model.*;
import com.escola.screi.model.enums.StatusMatricula;
import com.escola.screi.repository.SolicitacaoMatriculaRepository;
import com.escola.screi.repository.AlunoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
@Service @Validated
public class SolicitacaoService {
    private final SolicitacaoMatriculaRepository repository;
    private final AlunoRepository alunos;
    public SolicitacaoService(SolicitacaoMatriculaRepository repository, AlunoRepository alunos) {
        this.repository=repository; this.alunos=alunos;
    }
    @Transactional
    public SolicitacaoMatricula enviar(@Valid SolicitacaoMatriculaFormDto form) {
        String cpfAluno=form.getAluno().getCpf().replaceAll("\\D", "");
        if (alunos.existsByCpf(cpfAluno)) throw new AlunoJaCadastradoException();
        SolicitacaoMatricula s=new SolicitacaoMatricula();
        form.getAluno().setId(null); form.getResponsavel().setId(null); form.getContatoEmergencia().setId(null);
        form.getAluno().setCpf(cpfAluno);
        form.getResponsavel().setCpf(form.getResponsavel().getCpf().replaceAll("\\D", ""));
        s.setAluno(form.getAluno()); s.getResponsaveis().add(form.getResponsavel());
        s.getContatosEmergencia().add(form.getContatoEmergencia()); s.setStatus(StatusMatricula.ENVIADO);
        try {
            return repository.saveAndFlush(s);
        } catch (DataIntegrityViolationException ex) {
            // The unique constraint also protects simultaneous submissions.
            for (Throwable causa=ex; causa!=null; causa=causa.getCause()) {
                if (causa instanceof org.hibernate.exception.ConstraintViolationException violacao &&
                    violacao.getConstraintName()!=null &&
                    violacao.getConstraintName().toLowerCase(java.util.Locale.ROOT).contains("uk_aluno_cpf")) {
                    throw new AlunoJaCadastradoException();
                }
            }
            throw ex;
        }
    }
    @Transactional(readOnly=true)
    public List<SolicitacaoMatricula> consultar(String consulta) {
        if (consulta == null || consulta.isBlank()) return List.of();
        String valor=consulta.trim();
        List<SolicitacaoMatricula> encontrados=valor.matches("[0-9.\\- ]+") && valor.replaceAll("\\D", "").length()==11
            ? repository.findDistinctByResponsaveisCpfOrderByDataSubmissaoDesc(valor.replaceAll("\\D", ""))
            : repository.findByProtocoloIgnoreCase(valor);
        encontrados.forEach(s -> s.getAluno().getNomeCompleto());
        return encontrados;
    }
    @Transactional(readOnly=true)
    public List<SolicitacaoMatricula> listar() { return repository.findAllByOrderByDataSubmissaoDesc(); }
    @Transactional(readOnly=true)
    public SolicitacaoMatricula detalhar(Long id) {
        var s=buscar(id); s.getResponsaveis().size(); s.getContatosEmergencia().size(); return s;
    }
    private SolicitacaoMatricula buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitação não encontrada"));
    }
    @Transactional
    public void atualizarStatus(Long id, StatusUpdateDto dto) {
        if (dto.getStatus()==null) throw new IllegalArgumentException("Informe o status.");
        if (dto.getStatus()==StatusMatricula.COM_PENDENCIA &&
            (dto.getJustificativaReprovacao()==null || dto.getJustificativaReprovacao().isBlank()))
            throw new IllegalArgumentException("Informe a justificativa da pendência.");
        if (dto.getJustificativaReprovacao()!=null && dto.getJustificativaReprovacao().length()>2000)
            throw new IllegalArgumentException("A justificativa deve ter no máximo 2000 caracteres.");
        var s=buscar(id); s.setStatus(dto.getStatus());
        s.setJustificativaReprovacao(dto.getStatus()==StatusMatricula.COM_PENDENCIA ? dto.getJustificativaReprovacao().trim() : null);
        repository.saveAndFlush(s);
    }
}
```

## `src/main/java/com/escola/screi/service/ViaCepService.java`

```java
package com.escola.screi.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
@Service
public class ViaCepService {
    private final RestTemplate restTemplate;
    public ViaCepService(RestTemplateBuilder builder) {
        restTemplate=builder.connectTimeout(Duration.ofSeconds(3)).readTimeout(Duration.ofSeconds(5)).build();
    }
    public Endereco consultar(String cep) {
        if (cep==null || !cep.matches("[0-9]{8}")) throw new IllegalArgumentException("Informe um CEP de 8 dígitos.");
        Endereco endereco=restTemplate.getForObject("https://viacep.com.br/ws/{cep}/json/",Endereco.class,cep);
        if (endereco==null || Boolean.TRUE.equals(endereco.erro())) throw new IllegalArgumentException("CEP não encontrado.");
        return endereco;
    }
    public record Endereco(String cep, String logradouro, String bairro, String localidade, String uf, Boolean erro) {}
}
```

## `src/main/resources/application.properties`

```properties
spring.application.name=SCREI
spring.datasource.url=jdbc:h2:mem:screi;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.open-in-view=false
spring.h2.console.enabled=false
spring.thymeleaf.cache=false
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
server.error.include-message=never
screi.secretaria.email=${SCREI_ADMIN_EMAIL:secretaria@escola.com}
screi.secretaria.senha=${SCREI_ADMIN_PASSWORD:admin123}
```

## `src/main/resources/static/css/app.css`

```css
:root { --bs-primary: #175b83; --bs-primary-rgb: 23,91,131; }
body { background: #f3f6fa; color: #25364a; }
main { min-height: 75vh; max-width: 1100px; }
.card { border: 1px solid #dce4ed; border-radius: 14px; box-shadow: 0 5px 22px #20364e08; }
.btn-primary { background: #175b83; border-color: #175b83; }
.navbar-brand small { font-size: .75rem; margin-left: .5rem; }
.login-card { max-width: 460px; }
.steps { display: flex; flex-wrap: wrap; gap: 1rem; padding: 0; list-style: none; margin: 2rem 0; }
.steps li { padding: .7rem 1rem; background: #e3ebf4; border-radius: 8px; }
.steps li.active { background: #175b83; color: white; }
fieldset[hidden] { display: none !important; }
legend { float: none; width: auto; }
th { white-space: nowrap; }
td, th { padding: 1rem !important; vertical-align: middle; }
@media(max-width: 576px) { .navbar .container { gap: 1rem; } h1 { font-size: 1.8rem; } }
```

## `src/main/resources/static/js/triagem.js`

```javascript
'use strict';
const statusField = document.getElementById('status');
const justificativa = document.getElementById('justificativa');
function updateRequired() { justificativa.required = statusField.value === 'COM_PENDENCIA'; }
statusField.addEventListener('change', updateRequired);
updateRequired();
```

## `src/main/resources/static/js/wizard.js`

```javascript
'use strict';
const form = document.getElementById('wizard');
const steps = [...form.querySelectorAll('[data-step]')];
const previous = document.getElementById('previous');
const next = document.getElementById('next');
const submit = document.getElementById('submit');
let current = 0;
function showStep(index) {
  current = index;
  steps.forEach((step, i) => { step.hidden = i !== current; });
  document.querySelectorAll('.steps li').forEach((item, i) => {
    item.classList.toggle('active', i === current);
    if (i === current) item.setAttribute('aria-current', 'step'); else item.removeAttribute('aria-current');
  });
  previous.hidden = current === 0;
  next.hidden = current === steps.length - 1;
  submit.hidden = current !== steps.length - 1;
}
function validateStep() {
  for (const input of steps[current].querySelectorAll('input')) {
    if (!input.checkValidity()) { input.reportValidity(); return false; }
  }
  return true;
}
form.noValidate = true;
previous.addEventListener('click', () => showStep(current - 1));
next.addEventListener('click', () => { if (validateStep()) showStep(current + 1); });
form.addEventListener('submit', event => {
  for (let i = 0; i < steps.length; i++) {
    const invalid = [...steps[i].querySelectorAll('input')].find(input => !input.checkValidity());
    if (invalid) { event.preventDefault(); showStep(i); invalid.reportValidity(); return; }
  }
  submit.disabled = true;
});
const serverError = [...steps].findIndex(step => [...step.querySelectorAll('.text-danger')].some(el => el.textContent.trim()));
showStep(serverError >= 0 ? serverError : 0);
const cep = document.getElementById('responsavel-cep');
const feedback = document.getElementById('cep-feedback');
let request;
cep.addEventListener('input', async () => {
  cep.value = cep.value.replace(/\D/g, '').slice(0, 8);
  if (request) request.abort();
  feedback.textContent = '';
  if (cep.value.length !== 8) return;
  request = new AbortController();
  const value = cep.value;
  feedback.textContent = 'Consultando CEP…';
  try {
    const response = await fetch('/responsavel/cep/' + value, { signal: request.signal });
    const data = await response.json();
    if (cep.value !== value) return;
    if (!response.ok) throw new Error(data.mensagem || 'Consulta indisponível. Preencha manualmente.');
    for (const [field, key] of Object.entries({logradouro:'logradouro', bairro:'bairro', cidade:'localidade', estado:'uf'})) {
      document.getElementById('responsavel-' + field).value = data[key] || '';
    }
    feedback.textContent = 'Endereço localizado. Confira e informe o número.';
  } catch (error) {
    if (error.name !== 'AbortError' && cep.value === value) feedback.textContent = error.message;
  }
});
```

## `src/main/resources/templates/detalhes.html`

```html
<!DOCTYPE html>
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>SCREI • Educação Infantil</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link th:href="@{/css/app.css}" rel="stylesheet"></head><body>
<nav class="navbar navbar-dark bg-primary"><div class="container"><a class="navbar-brand" th:href="@{/}">SCREI <small>Educação Infantil</small></a><div class="d-flex gap-3"><a class="text-white" th:href="@{/responsavel/novo}">Cadastro</a><a class="text-white" th:href="@{/responsavel/status}">Acompanhar</a><a class="text-white" th:href="@{/secretaria/triagem}">Secretaria</a></div></div></nav><main class="container py-5">
<a th:href="@{/secretaria/triagem}">← Voltar à triagem</a><h1 class="mt-3">Detalhes da solicitação</h1><p class="text-break" th:text="${solicitacao.protocolo}"></p><div th:if="${sucesso}" th:text="${sucesso}" class="alert alert-success"></div><p>Situação atual: <strong th:text="${solicitacao.status}"></strong></p><p th:if="${solicitacao.justificativaReprovacao != null}" th:text="${solicitacao.justificativaReprovacao}" class="alert alert-warning"></p><section class="card p-4 mb-3"><h2 class="h4">Dados do aluno</h2><dl class="row mb-0"><dt class="col-sm-4">Nome completo</dt><dd class="col-sm-8 text-break" th:text="${solicitacao.aluno.nomeCompleto}"></dd><dt class="col-sm-4">Data de nascimento</dt><dd class="col-sm-8 text-break" th:text="${solicitacao.aluno.dataNascimento}"></dd><dt class="col-sm-4">Certidão de nascimento</dt><dd class="col-sm-8 text-break" th:text="${solicitacao.aluno.certidaoNascimento}"></dd><dt class="col-sm-4">CPF do aluno</dt><dd class="col-sm-8 text-break" th:text="${solicitacao.aluno.cpf}"></dd><dt class="col-sm-4">Pessoas autorizadas a buscar</dt><dd class="col-sm-8 text-break" th:text="${solicitacao.aluno.autorizadosBuscar}"></dd></dl></section><section class="card p-4 mb-3" th:each="r : ${solicitacao.responsaveis}"><h2 class="h4">Dados do responsável</h2><dl class="row mb-0"><dt class="col-sm-4">Nome completo</dt><dd class="col-sm-8 text-break" th:text="${r.nomeCompleto}"></dd><dt class="col-sm-4">Parentesco</dt><dd class="col-sm-8 text-break" th:text="${r.parentesco}"></dd><dt class="col-sm-4">CPF</dt><dd class="col-sm-8 text-break" th:text="${r.cpf}"></dd><dt class="col-sm-4">Telefone com DDD</dt><dd class="col-sm-8 text-break" th:text="${r.telefone}"></dd><dt class="col-sm-4">E-mail</dt><dd class="col-sm-8 text-break" th:text="${r.email}"></dd><dt class="col-sm-4">CEP (8 dígitos)</dt><dd class="col-sm-8 text-break" th:text="${r.cep}"></dd><dt class="col-sm-4">Logradouro</dt><dd class="col-sm-8 text-break" th:text="${r.logradouro}"></dd><dt class="col-sm-4">Número / sem número</dt><dd class="col-sm-8 text-break" th:text="${r.numero}"></dd><dt class="col-sm-4">Bairro</dt><dd class="col-sm-8 text-break" th:text="${r.bairro}"></dd><dt class="col-sm-4">Cidade</dt><dd class="col-sm-8 text-break" th:text="${r.cidade}"></dd><dt class="col-sm-4">UF</dt><dd class="col-sm-8 text-break" th:text="${r.estado}"></dd></dl></section><section class="card p-4 mb-3" th:each="c : ${solicitacao.contatosEmergencia}"><h2 class="h4">Contato de emergência</h2><dl class="row mb-0"><dt class="col-sm-4">Nome completo</dt><dd class="col-sm-8 text-break" th:text="${c.nome}"></dd><dt class="col-sm-4">Parentesco</dt><dd class="col-sm-8 text-break" th:text="${c.parentesco}"></dd><dt class="col-sm-4">Telefone com DDD</dt><dd class="col-sm-8 text-break" th:text="${c.telefone}"></dd></dl></section><section class="card p-4"><h2 class="h4">Atualizar situação</h2><form th:action="@{/secretaria/solicitacoes/{id}/status(id=${solicitacao.id})}" th:object="${atualizacao}" method="post"><div th:if="${#fields.hasAnyErrors()}" class="alert alert-danger" role="alert"><p th:each="err : ${#fields.allErrors()}" th:text="${err}"></p></div><label for="status" class="form-label">Nova situação</label><select id="status" th:field="*{status}" class="form-select mb-3" required><option value="">Selecione</option><option th:each="s : ${statusDisponiveis}" th:value="${s}" th:text="${s}"></option></select><label for="justificativa" class="form-label">Justificativa (obrigatória para pendência)</label><textarea id="justificativa" th:field="*{justificativaReprovacao}" class="form-control mb-3" maxlength="2000" rows="4"></textarea><button class="btn btn-primary">Salvar situação</button></form></section><script th:src="@{/js/triagem.js}" defer></script></main><footer class="container pb-4 text-secondary">SCREI · Sistema de Cadastro e Rematrícula da Educação Infantil</footer></body></html>
```

## `src/main/resources/templates/login.html`

```html
<!DOCTYPE html>
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>SCREI • Educação Infantil</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link th:href="@{/css/app.css}" rel="stylesheet"></head><body>
<nav class="navbar navbar-dark bg-primary"><div class="container"><a class="navbar-brand" th:href="@{/}">SCREI <small>Educação Infantil</small></a><div class="d-flex gap-3"><a class="text-white" th:href="@{/responsavel/novo}">Cadastro</a><a class="text-white" th:href="@{/responsavel/status}">Acompanhar</a><a class="text-white" th:href="@{/secretaria/triagem}">Secretaria</a></div></div></nav><main class="container py-5">

<section class="card mx-auto login-card"><div class="card-body p-4"><h1 class="h3">Acesso da secretaria</h1><p class="text-secondary">Entre para analisar as solicitações de matrícula.</p>
<p th:if="${param.error}" class="alert alert-danger" role="alert">E-mail ou senha inválidos.</p><p th:if="${param.logout}" class="alert alert-success">Você saiu da sua conta.</p>
<form th:action="@{/login}" method="post"><label for="email" class="form-label">E-mail</label><input id="email" name="email" type="email" class="form-control mb-3" required autocomplete="username"><label for="password" class="form-label">Senha</label><input id="password" name="password" type="password" class="form-control mb-4" required autocomplete="current-password"><button class="btn btn-primary w-100">Entrar</button></form></div></section>
</main><footer class="container pb-4 text-secondary">SCREI · Sistema de Cadastro e Rematrícula da Educação Infantil</footer></body></html>
```

## `src/main/resources/templates/status.html`

```html
<!DOCTYPE html>
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>SCREI • Educação Infantil</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link th:href="@{/css/app.css}" rel="stylesheet"></head><body>
<nav class="navbar navbar-dark bg-primary"><div class="container"><a class="navbar-brand" th:href="@{/}">SCREI <small>Educação Infantil</small></a><div class="d-flex gap-3"><a class="text-white" th:href="@{/responsavel/novo}">Cadastro</a><a class="text-white" th:href="@{/responsavel/status}">Acompanhar</a><a class="text-white" th:href="@{/secretaria/triagem}">Secretaria</a></div></div></nav><main class="container py-5">

<h1>Acompanhar solicitação</h1><p>Informe o protocolo ou o CPF do responsável.</p><div th:if="${sucesso}" th:text="${sucesso}" class="alert alert-success" role="status"></div>
<form th:action="@{/responsavel/status}" method="get" class="d-flex gap-2 mb-4"><label for="consulta" class="visually-hidden">Protocolo ou CPF</label><input id="consulta" name="consulta" th:value="${consulta}" class="form-control" maxlength="60" required placeholder="Protocolo ou CPF do responsável"><button class="btn btn-primary">Consultar</button></form>
<p th:if="${!#strings.isEmpty(consulta) and #lists.isEmpty(solicitacoes)}" class="alert alert-info">Nenhuma solicitação encontrada.</p>
<article th:each="s : ${solicitacoes}" class="card mb-3"><div class="card-body"><h2 class="h5">Solicitação de matrícula</h2><p class="text-break">Protocolo: <strong th:text="${s.protocolo}"></strong></p><p>Situação: <span class="badge text-bg-primary" th:text="${s.status}"></span></p><p>Enviada em <span th:text="${#temporals.format(s.dataSubmissao, 'dd/MM/yyyy HH:mm')}"></span></p><p>Atualizada em <span th:text="${#temporals.format(s.dataAtualizacao, 'dd/MM/yyyy HH:mm')}"></span></p><div th:if="${s.justificativaReprovacao != null}" class="alert alert-warning"><strong>Pendência: </strong><span th:text="${s.justificativaReprovacao}"></span></div></div></article>
</main><footer class="container pb-4 text-secondary">SCREI · Sistema de Cadastro e Rematrícula da Educação Infantil</footer></body></html>
```

## `src/main/resources/templates/triagem.html`

```html
<!DOCTYPE html>
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>SCREI • Educação Infantil</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link th:href="@{/css/app.css}" rel="stylesheet"></head><body>
<nav class="navbar navbar-dark bg-primary"><div class="container"><a class="navbar-brand" th:href="@{/}">SCREI <small>Educação Infantil</small></a><div class="d-flex gap-3"><a class="text-white" th:href="@{/responsavel/novo}">Cadastro</a><a class="text-white" th:href="@{/responsavel/status}">Acompanhar</a><a class="text-white" th:href="@{/secretaria/triagem}">Secretaria</a></div></div></nav><main class="container py-5">

<div class="d-flex flex-wrap justify-content-between gap-3 mb-4"><div><h1>Fila de triagem</h1><p class="text-secondary">Analise os cadastros e acompanhe a situação das matrículas.</p></div><div class="d-flex align-items-start gap-2"><a class="btn btn-success" th:href="@{/secretaria/exportar-excel}">Exportar aprovados (.xlsx)</a><form th:action="@{/logout}" method="post"><button class="btn btn-outline-secondary">Sair</button></form></div></div>
<div class="card table-responsive"><table class="table table-hover mb-0"><thead><tr><th>Protocolo</th><th>Aluno</th><th>Submissão</th><th>Situação</th><th>Ação</th></tr></thead><tbody><tr th:if="${#lists.isEmpty(solicitacoes)}"><td colspan="5" class="text-center p-4">Nenhuma solicitação recebida.</td></tr><tr th:each="s : ${solicitacoes}"><td class="text-break" th:text="${s.protocolo}"></td><td th:text="${s.aluno.nomeCompleto}"></td><td th:text="${#temporals.format(s.dataSubmissao, 'dd/MM/yyyy HH:mm')}"></td><td><span class="badge text-bg-primary" th:text="${s.status}"></span></td><td><a th:href="@{/secretaria/solicitacoes/{id}(id=${s.id})}" class="btn btn-sm btn-outline-primary">Detalhar</a></td></tr></tbody></table></div>
</main><footer class="container pb-4 text-secondary">SCREI · Sistema de Cadastro e Rematrícula da Educação Infantil</footer></body></html>
```

## `src/main/resources/templates/wizard.html`

```html
<!DOCTYPE html>
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>SCREI • Educação Infantil</title><link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"><link th:href="@{/css/app.css}" rel="stylesheet"></head><body>
<nav class="navbar navbar-dark bg-primary"><div class="container"><a class="navbar-brand" th:href="@{/}">SCREI <small>Educação Infantil</small></a><div class="d-flex gap-3"><a class="text-white" th:href="@{/responsavel/novo}">Cadastro</a><a class="text-white" th:href="@{/responsavel/status}">Acompanhar</a><a class="text-white" th:href="@{/secretaria/triagem}">Secretaria</a></div></div></nav><main class="container py-5">
<h1>Solicitação de matrícula</h1><p class="text-secondary">Preencha os dados e guarde o protocolo para acompanhar a análise.</p><ol class="steps"><li>1. Aluno</li><li>2. Responsável</li><li>3. Emergência</li></ol><form id="wizard" th:action="@{/responsavel/solicitacoes}" th:object="${form}" method="post"><div th:if="${#fields.hasAnyErrors()}" class="alert alert-danger" role="alert">Confira os campos indicados. Seus dados foram preservados.</div><fieldset data-step="0" class="card p-4 mb-3"><legend class="h4">Dados do aluno</legend><div class="row g-3"><div class="col-md-6"><label for="aluno-nomeCompleto" class="form-label">Nome completo</label><input id="aluno-nomeCompleto" type="text" th:field="*{aluno.nomeCompleto}" class="form-control" required maxlength="200"><div class="text-danger small" th:errors="*{aluno.nomeCompleto}"></div></div><div class="col-md-6"><label for="aluno-dataNascimento" class="form-label">Data de nascimento</label><input id="aluno-dataNascimento" type="date" th:field="*{aluno.dataNascimento}" class="form-control" required ><div class="text-danger small" th:errors="*{aluno.dataNascimento}"></div></div><div class="col-md-6"><label for="aluno-certidaoNascimento" class="form-label">Certidão de nascimento</label><input id="aluno-certidaoNascimento" type="text" th:field="*{aluno.certidaoNascimento}" class="form-control" required maxlength="100"><div class="text-danger small" th:errors="*{aluno.certidaoNascimento}"></div></div><div class="col-md-6"><label for="aluno-cpf" class="form-label">CPF do aluno</label><input id="aluno-cpf" type="text" th:field="*{aluno.cpf}" class="form-control" required maxlength="14"><div class="text-danger small" th:errors="*{aluno.cpf}"></div></div><div class="col-md-6"><label for="aluno-autorizadosBuscar" class="form-label">Pessoas autorizadas a buscar</label><input id="aluno-autorizadosBuscar" type="text" th:field="*{aluno.autorizadosBuscar}" class="form-control" required maxlength="2000"><div class="text-danger small" th:errors="*{aluno.autorizadosBuscar}"></div></div></div></fieldset><fieldset data-step="1" class="card p-4 mb-3"><legend class="h4">Dados do responsável</legend><div class="row g-3"><div class="col-md-6"><label for="responsavel-nomeCompleto" class="form-label">Nome completo</label><input id="responsavel-nomeCompleto" type="text" th:field="*{responsavel.nomeCompleto}" class="form-control" required maxlength="200"><div class="text-danger small" th:errors="*{responsavel.nomeCompleto}"></div></div><div class="col-md-6"><label for="responsavel-parentesco" class="form-label">Parentesco</label><input id="responsavel-parentesco" type="text" th:field="*{responsavel.parentesco}" class="form-control" required maxlength="100"><div class="text-danger small" th:errors="*{responsavel.parentesco}"></div></div><div class="col-md-6"><label for="responsavel-cpf" class="form-label">CPF</label><input id="responsavel-cpf" type="text" th:field="*{responsavel.cpf}" class="form-control" required maxlength="14"><div class="text-danger small" th:errors="*{responsavel.cpf}"></div></div><div class="col-md-6"><label for="responsavel-telefone" class="form-label">Telefone com DDD</label><input id="responsavel-telefone" type="tel" th:field="*{responsavel.telefone}" class="form-control" required maxlength="20"><div class="text-danger small" th:errors="*{responsavel.telefone}"></div></div><div class="col-md-6"><label for="responsavel-email" class="form-label">E-mail</label><input id="responsavel-email" type="email" th:field="*{responsavel.email}" class="form-control" required maxlength="254"><div class="text-danger small" th:errors="*{responsavel.email}"></div></div><div class="col-md-6"><label for="responsavel-cep" class="form-label">CEP (8 dígitos)</label><input id="responsavel-cep" type="text" th:field="*{responsavel.cep}" class="form-control" required inputmode="numeric" maxlength="8" pattern="[0-9]{8}"><div class="text-danger small" th:errors="*{responsavel.cep}"></div></div><div class="col-md-6"><label for="responsavel-logradouro" class="form-label">Logradouro</label><input id="responsavel-logradouro" type="text" th:field="*{responsavel.logradouro}" class="form-control" required maxlength="200"><div class="text-danger small" th:errors="*{responsavel.logradouro}"></div></div><div class="col-md-6"><label for="responsavel-numero" class="form-label">Número / sem número</label><input id="responsavel-numero" type="text" th:field="*{responsavel.numero}" class="form-control" required maxlength="20"><div class="text-danger small" th:errors="*{responsavel.numero}"></div></div><div class="col-md-6"><label for="responsavel-bairro" class="form-label">Bairro</label><input id="responsavel-bairro" type="text" th:field="*{responsavel.bairro}" class="form-control" required maxlength="100"><div class="text-danger small" th:errors="*{responsavel.bairro}"></div></div><div class="col-md-6"><label for="responsavel-cidade" class="form-label">Cidade</label><input id="responsavel-cidade" type="text" th:field="*{responsavel.cidade}" class="form-control" required maxlength="100"><div class="text-danger small" th:errors="*{responsavel.cidade}"></div></div><div class="col-md-6"><label for="responsavel-estado" class="form-label">UF</label><input id="responsavel-estado" type="text" th:field="*{responsavel.estado}" class="form-control" required maxlength="2" pattern="[A-Z]{2}"><div class="text-danger small" th:errors="*{responsavel.estado}"></div></div></div><p id="cep-feedback" role="status" aria-live="polite" class="mt-3 mb-0 text-secondary"></p></fieldset><fieldset data-step="2" class="card p-4 mb-3"><legend class="h4">Contato de emergência</legend><div class="row g-3"><div class="col-md-6"><label for="contatoEmergencia-nome" class="form-label">Nome completo</label><input id="contatoEmergencia-nome" type="text" th:field="*{contatoEmergencia.nome}" class="form-control" required maxlength="200"><div class="text-danger small" th:errors="*{contatoEmergencia.nome}"></div></div><div class="col-md-6"><label for="contatoEmergencia-parentesco" class="form-label">Parentesco</label><input id="contatoEmergencia-parentesco" type="text" th:field="*{contatoEmergencia.parentesco}" class="form-control" required maxlength="100"><div class="text-danger small" th:errors="*{contatoEmergencia.parentesco}"></div></div><div class="col-md-6"><label for="contatoEmergencia-telefone" class="form-label">Telefone com DDD</label><input id="contatoEmergencia-telefone" type="tel" th:field="*{contatoEmergencia.telefone}" class="form-control" required maxlength="20"><div class="text-danger small" th:errors="*{contatoEmergencia.telefone}"></div></div></div></fieldset><div class="d-flex gap-2"><button type="button" id="previous" class="btn btn-outline-secondary" hidden>Voltar</button><button type="button" id="next" class="btn btn-primary" hidden>Próximo</button><button type="submit" id="submit" class="btn btn-success">Enviar solicitação</button></div></form><script th:src="@{/js/wizard.js}" defer></script></main><footer class="container pb-4 text-secondary">SCREI · Sistema de Cadastro e Rematrícula da Educação Infantil</footer></body></html>
```

## `src/test/java/com/escola/screi/ScreiApplicationTests.java`

```java
package com.escola.screi;

import com.escola.screi.dto.*;
import com.escola.screi.model.enums.StatusMatricula;
import com.escola.screi.repository.*;
import com.escola.screi.service.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ScreiApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired SolicitacaoService service;
    @Autowired SolicitacaoMatriculaRepository repository;
    @Autowired UsuarioRepository usuarios;
    @Autowired ExcelExportService excel;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired AlunoRepository alunos;

    @BeforeEach void limparSolicitacoes() {
        repository.deleteAll();
        alunos.deleteAll();
    }

    private SolicitacaoMatriculaFormDto formulario() {
        var form = new SolicitacaoMatriculaFormDto();
        var a = form.getAluno();
        a.setNomeCompleto("Criança Teste"); a.setDataNascimento(LocalDate.now().minusYears(3));
        a.setCertidaoNascimento("123456789"); a.setCpf("529.982.247-25"); a.setAutorizadosBuscar("Maria Teste");
        var r = form.getResponsavel();
        r.setNomeCompleto("Maria Teste"); r.setParentesco("Mãe"); r.setCpf("111.444.777-35");
        r.setTelefone("71999999999"); r.setEmail("maria@example.com"); r.setCep("40020000");
        r.setLogradouro("Rua Teste"); r.setNumero("10"); r.setBairro("Centro"); r.setCidade("Salvador"); r.setEstado("BA");
        var c = form.getContatoEmergencia();
        c.setNome("João Teste"); c.setParentesco("Tio"); c.setTelefone("71988888888");
        return form;
    }

    @Test void cadastroConsultaEDetalhes() throws Exception {
        var s = service.enviar(formulario());
        assertThat(s.getProtocolo()).isNotBlank();
        assertThat(s.getStatus()).isEqualTo(StatusMatricula.ENVIADO);
        assertThat(s.getDataSubmissao()).isNotNull();
        assertThat(service.consultar(s.getProtocolo())).extracting("id").contains(s.getId());
        assertThat(service.consultar("111.444.777-35")).extracting("id").contains(s.getId());
        assertThat(service.detalhar(s.getId()).getResponsaveis()).hasSize(1);
        mvc.perform(get("/responsavel/status").param("consulta",s.getProtocolo()))
            .andExpect(status().isOk()).andExpect(view().name("status"));
        mvc.perform(get("/secretaria/triagem").with(user("secretaria").roles("SECRETARIA")))
            .andExpect(status().isOk());
        mvc.perform(get("/secretaria/solicitacoes/"+s.getId()).with(user("secretaria").roles("SECRETARIA")))
            .andExpect(status().isOk()).andExpect(view().name("detalhes"));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\t\n"})
    void bloqueiaPendenciaSemJustificativa(String justificativa) {
        var s=service.enviar(formulario());
        var dto=new StatusUpdateDto(); dto.setStatus(StatusMatricula.COM_PENDENCIA); dto.setJustificativaReprovacao(justificativa);
        assertThatThrownBy(() -> service.atualizarStatus(s.getId(),dto)).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.findById(s.getId()).orElseThrow().getStatus()).isEqualTo(StatusMatricula.ENVIADO);
    }

    @Test void workflowEExportacaoSomenteAprovados() throws Exception {
        var aprovado=service.enviar(formulario());
        var segundoFormulario=formulario();
        segundoFormulario.getAluno().setCpf("111.444.777-35");
        var pendente=service.enviar(segundoFormulario);
        var dto=new StatusUpdateDto(); dto.setStatus(StatusMatricula.EM_ANALISE); service.atualizarStatus(aprovado.getId(),dto);
        dto.setStatus(StatusMatricula.APROVADO); service.atualizarStatus(aprovado.getId(),dto);
        dto.setStatus(StatusMatricula.COM_PENDENCIA); dto.setJustificativaReprovacao(" Apresentar certidão legível. ");
        service.atualizarStatus(pendente.getId(),dto);
        assertThat(service.detalhar(pendente.getId()).getJustificativaReprovacao()).isEqualTo("Apresentar certidão legível.");
        try (var workbook=new XSSFWorkbook(new ByteArrayInputStream(excel.exportar()))) {
            var sheet=workbook.getSheetAt(0);
            var protocolos=new java.util.ArrayList<String>();
            for (int i=1;i<=sheet.getLastRowNum();i++) {
                protocolos.add(sheet.getRow(i).getCell(0).getStringCellValue());
                assertThat(sheet.getRow(i).getCell(12).getStringCellValue()).isEqualTo("APROVADO");
            }
            assertThat(protocolos).contains(aprovado.getProtocolo()).doesNotContain(pendente.getProtocolo());
        }
        dto.setStatus(StatusMatricula.APROVADO); service.atualizarStatus(pendente.getId(),dto);
        assertThat(service.detalhar(pendente.getId()).getJustificativaReprovacao()).isNull();
    }

    @Test void paginasPublicasEControleDeAcesso() throws Exception {
        mvc.perform(get("/responsavel/novo")).andExpect(status().isOk()).andExpect(view().name("wizard"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/secretaria/triagem")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/secretaria/exportar-excel").with(user("pai").roles("RESPONSAVEL"))).andExpect(status().isForbidden());
        mvc.perform(post("/responsavel/solicitacoes")).andExpect(status().isForbidden());
        mvc.perform(post("/responsavel/solicitacoes").with(csrf())).andExpect(status().isOk()).andExpect(model().hasErrors());
        mvc.perform(get("/responsavel/cep/123")).andExpect(status().isBadRequest());
        var admin=usuarios.findByEmailIgnoreCase("secretaria@escola.com").orElseThrow();
        assertThat(encoder.matches("admin123",admin.getSenha())).isTrue();
        mvc.perform(post("/login").with(csrf()).param("email",admin.getEmail()).param("password","admin123"))
            .andExpect(redirectedUrl("/secretaria/triagem"));
    }

    @Test void controllerRejeitaPendenciaSemJustificativa() throws Exception {
        var s=service.enviar(formulario());
        mvc.perform(post("/secretaria/solicitacoes/"+s.getId()+"/status")
            .with(user("secretaria").roles("SECRETARIA")).with(csrf())
            .param("status","COM_PENDENCIA").param("justificativaReprovacao"," "))
            .andExpect(status().isOk()).andExpect(model().hasErrors()).andExpect(view().name("detalhes"));
        assertThat(service.detalhar(s.getId()).getStatus()).isEqualTo(StatusMatricula.ENVIADO);
    }

    @Test void submissaoHttpValidaDataCpfECamposPermitidos() throws Exception {
        mvc.perform(submissaoValida())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/responsavel/status?consulta=*"));
        var s=service.consultar("11144477735").stream()
            .filter(item -> item.getAluno().getNomeCompleto().equals("Aluno do formulário")).findFirst().orElseThrow();
        assertThat(s.getAluno().getId()).isNotEqualTo(99999L);
        assertThat(s.getAluno().getDataNascimento()).isEqualTo(LocalDate.of(2023,1,15));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submissaoValida() {
        return post("/responsavel/solicitacoes").with(csrf())
            .param("aluno.nomeCompleto","Aluno do formulário").param("aluno.dataNascimento","2023-01-15")
            .param("aluno.certidaoNascimento","123456").param("aluno.cpf","529.982.247-25")
            .param("aluno.autorizadosBuscar","Maria").param("aluno.id","99999")
            .param("responsavel.nomeCompleto","Maria").param("responsavel.parentesco","Mãe")
            .param("responsavel.cpf","111.444.777-35").param("responsavel.telefone","71999999999")
            .param("responsavel.email","maria@example.com").param("responsavel.cep","40020000")
            .param("responsavel.logradouro","Rua Teste").param("responsavel.numero","10")
            .param("responsavel.bairro","Centro").param("responsavel.cidade","Salvador").param("responsavel.estado","BA")
            .param("contatoEmergencia.nome","João").param("contatoEmergencia.parentesco","Tio")
            .param("contatoEmergencia.telefone","71988888888");
    }

    @ParameterizedTest @ValueSource(strings={"529.982.247-25", "52998224725"})
    void rejeitaCpfDeAlunoRepetidoMesmoComOutroResponsavel(String cpf) {
        service.enviar(formulario());
        var duplicado=formulario();
        duplicado.getAluno().setCpf(cpf);
        duplicado.getResponsavel().setCpf("52998224725");
        assertThatThrownBy(() -> service.enviar(duplicado)).isInstanceOf(AlunoJaCadastradoException.class);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(alunos.count()).isEqualTo(1);
    }

    @Test void permiteIrmaosComMesmoResponsavel() {
        var primeiro=service.enviar(formulario());
        var segundoForm=formulario();
        segundoForm.getAluno().setCpf("11144477735");
        var segundo=service.enviar(segundoForm);
        assertThat(repository.count()).isEqualTo(2);
        assertThat(service.consultar("11144477735")).extracting("id").containsExactlyInAnyOrder(primeiro.getId(),segundo.getId());
    }

    @Test void bancoImpedeDuplicidadeMesmoSemPassarPeloServico() {
        service.enviar(formulario());
        var duplicado=formulario().getAluno();
        assertThatThrownBy(() -> alunos.saveAndFlush(duplicado))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(alunos.count()).isEqualTo(1);
    }

    @Test void formularioExibeErroNoCpfEPreservaDados() throws Exception {
        service.enviar(formulario());
        mvc.perform(submissaoValida()).andExpect(status().isOk()).andExpect(view().name("wizard"))
            .andExpect(model().attributeHasFieldErrorCode("form","aluno.cpf","aluno.cpf.duplicado"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Já existe um aluno cadastrado com este CPF")))
            .andExpect(result -> assertThat(org.unbescape.html.HtmlEscape.unescapeHtml(
                result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)))
                .contains("value=\"Aluno do formulário\""));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test void enviosSimultaneosGeramUmaUnicaSolicitacao() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        var pronto=new java.util.concurrent.CountDownLatch(2);
        var iniciar=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> enviar=() -> {
            var form=formulario();
            pronto.countDown();
            if (!iniciar.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Timeout");
            try { service.enviar(form); return true; }
            catch (AlunoJaCadastradoException ex) { return false; }
        };
        try {
            var primeiro=executor.submit(enviar); var segundo=executor.submit(enviar);
            assertThat(pronto.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            assertThat(java.util.List.of(primeiro.get(20,java.util.concurrent.TimeUnit.SECONDS),
                segundo.get(20,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(alunos.count()).isEqualTo(1);
        } finally { iniciar.countDown(); executor.shutdownNow(); }
    }
}
```

## `src/test/java/com/escola/screi/ViaCepServiceTests.java`

```java
package com.escola.screi;

import com.escola.screi.service.ViaCepService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepServiceTests {
    @Test void consultaEnderecoETrataCepInexistente() {
        var customizer=new MockServerRestTemplateCustomizer();
        var service=new ViaCepService(new RestTemplateBuilder(customizer));
        customizer.getServer().expect(requestTo("https://viacep.com.br/ws/40020000/json/"))
            .andRespond(withSuccess("{\"cep\":\"40020-000\",\"logradouro\":\"Rua Teste\",\"bairro\":\"Centro\",\"localidade\":\"Salvador\",\"uf\":\"BA\"}",MediaType.APPLICATION_JSON));
        assertThat(service.consultar("40020000").localidade()).isEqualTo("Salvador");
        customizer.getServer().verify();
        customizer.getServer().reset();
        customizer.getServer().expect(requestTo("https://viacep.com.br/ws/00000000/json/"))
            .andRespond(withSuccess("{\"erro\":true}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.consultar("00000000")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultar("abc")).isInstanceOf(IllegalArgumentException.class);
        customizer.getServer().verify();
    }
}
```

## `tools/export_source.py`

```python
"""Generate the complete Markdown source delivery from the files on disk."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
files = [root / 'pom.xml', root / '.mvn/wrapper/maven-wrapper.properties']
files += sorted(path for path in (root / 'src').rglob('*') if path.is_file())
files += [Path(__file__).resolve()]
languages = {'.xml': 'xml', '.properties': 'properties', '.java': 'java',
             '.html': 'html', '.css': 'css', '.js': 'javascript', '.py': 'python'}
parts = ['# SCREI — código-fonte integral\n',
         'Cada seção contém um arquivo completo do projeto, sem omissões. '
         'Consulte `README.md` para execução, credenciais e configuração.\n']
for path in files:
    relative = path.relative_to(root).as_posix()
    language = languages.get(path.suffix, 'text')
    parts.append(f'## `{relative}`\n\n```{language}\n{path.read_text(encoding="utf-8").rstrip()}\n```\n')
(root / 'CODIGO_COMPLETO.md').write_text('\n'.join(parts), encoding='utf-8')
print(f'{len(files)} arquivos exportados integralmente para CODIGO_COMPLETO.md')
```
