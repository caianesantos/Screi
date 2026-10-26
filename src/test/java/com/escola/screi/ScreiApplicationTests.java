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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
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
    @MockitoBean DocumentoStorage storage;

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
        ImagensTeste.anexar(form);
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
        return submissao(true);
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submissao(boolean comImagens) {
        var request=multipart("/responsavel/solicitacoes");
        if (comImagens) ImagensTeste.anexar(request);
        return request.with(csrf())
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

    @Test void imagensSaoAssociadasEVisiveisSomentePelaSecretaria() throws Exception {
        var s=service.enviar(formulario());
        var documentos=service.detalhar(s.getId()).getDocumentos();
        assertThat(documentos).hasSize(4);
        verify(storage,times(4)).enviar(anyString(),any(byte[].class),eq("image/png"));
        var documento=documentos.get(0);
        when(storage.ler(documento.getChaveBlob())).thenReturn(ImagensTeste.bytes("png"));
        String url="/secretaria/solicitacoes/"+s.getId()+"/documentos/"+documento.getId();
        mvc.perform(get(url)).andExpect(status().is3xxRedirection());
        mvc.perform(get(url).with(user("pai").roles("RESPONSAVEL"))).andExpect(status().isForbidden());
        mvc.perform(get(url).with(user("secretaria").roles("SECRETARIA"))).andExpect(status().isOk())
            .andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(content().bytes(ImagensTeste.bytes("png")));
        mvc.perform(get("/secretaria/solicitacoes/999999/documentos/"+documento.getId())
            .with(user("secretaria").roles("SECRETARIA"))).andExpect(status().isNotFound());
        mvc.perform(get("/secretaria/solicitacoes/"+s.getId()).with(user("secretaria").roles("SECRETARIA")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Documentos enviados")));
    }

    @Test void imagensObrigatoriasNaoPodemSerOmitidasNoBackend() throws Exception {
        mvc.perform(submissao(false)).andExpect(status().isOk()).andExpect(view().name("wizard"))
            .andExpect(model().attributeHasFieldErrorCode("form","cpfAlunoImagem","documento.invalido"));
        assertThat(repository.count()).isZero();
        verifyNoInteractions(storage);
    }

    @Test void falhaDeUploadDesfazCadastroERemoveArquivosParciais() {
        var contador=new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            if (contador.incrementAndGet()==2) throw new ArmazenamentoIndisponivelException(new IllegalStateException("Falha simulada"));
            return null;
        }).when(storage).enviar(anyString(),any(byte[].class),anyString());
        assertThatThrownBy(() -> service.enviar(formulario())).isInstanceOf(ArmazenamentoIndisponivelException.class);
        assertThat(repository.count()).isZero();
        assertThat(alunos.count()).isZero();
        verify(storage,times(2)).excluir(anyString());
    }

    @Test void falhaDeArmazenamentoMostraMensagemSemConcluirCadastro() throws Exception {
        doThrow(new ArmazenamentoIndisponivelException(new IllegalStateException("Falha simulada")))
            .when(storage).enviar(anyString(),any(byte[].class),anyString());
        mvc.perform(submissaoValida()).andExpect(status().isOk()).andExpect(view().name("wizard"))
            .andExpect(model().hasErrors()).andExpect(content().string(org.hamcrest.Matchers.containsString("temporariamente indisponível")));
        assertThat(repository.count()).isZero();
    }

    @Test void ausenciaDeRecursoEConvertidaParaHttpSomenteNaCamadaWeb() throws Exception {
        assertThatThrownBy(() -> service.detalhar(-1L)).isInstanceOf(RecursoNaoEncontradoException.class);
        mvc.perform(get("/secretaria/solicitacoes/-1").with(user("secretaria").roles("SECRETARIA")))
            .andExpect(status().isNotFound());
    }
}
