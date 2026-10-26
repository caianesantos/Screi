package com.escola.screi;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.escola.screi.service.AzureBlobDocumentoStorage;
import com.escola.screi.service.SolicitacaoService;
import com.escola.screi.repository.SolicitacaoMatriculaRepository;
import com.escola.screi.dto.SolicitacaoMatriculaFormDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SCREI_TEST_AZURITE",matches="true")
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:azurite-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AzuriteIntegrationTests {
    private static final String CONTAINER="screi-flow-test-"+java.util.UUID.randomUUID();
    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry) {
        registry.add("screi.storage.container",() -> CONTAINER);
    }
    @Autowired SolicitacaoService solicitacoes;
    @Autowired SolicitacaoMatriculaRepository repository;
    @Autowired MockMvc mvc;

    @Test void enviaLeEExcluiImagemNoEmuladorComContainerPrivado() {
        String connection=System.getenv().getOrDefault("AZURE_STORAGE_CONNECTION_STRING","UseDevelopmentStorage=true");
        String nome="screi-test-"+java.util.UUID.randomUUID();
        var client=new BlobServiceClientBuilder().connectionString(connection)
            .serviceVersion(BlobServiceVersion.V2023_11_03).buildClient().getBlobContainerClient(nome);
        var storage=new AzureBlobDocumentoStorage(client);
        try {
            byte[] imagem=ImagensTeste.bytes("png");
            storage.enviar("cadastro/imagem.png",imagem,"image/png");
            assertThat(storage.ler("cadastro/imagem.png")).isEqualTo(imagem);
            assertThat(client.getAccessPolicy().getBlobAccessType()).isNull();
            assertThat(client.getBlobClient("cadastro/imagem.png").getProperties().getContentType()).isEqualTo("image/png");
            storage.excluir("cadastro/imagem.png");
            assertThat(client.getBlobClient("cadastro/imagem.png").exists()).isFalse();
            ContratoDocumentoStorage.verificar(storage);
        } finally { client.deleteIfExists(); }
    }

    @Test void cadastroCompletoGuardaQuatroImagensELiberaLeituraAutenticada() throws Exception {
        String connection=System.getenv().getOrDefault("AZURE_STORAGE_CONNECTION_STRING","UseDevelopmentStorage=true");
        var client=new BlobServiceClientBuilder().connectionString(connection)
            .serviceVersion(BlobServiceVersion.V2023_11_03).buildClient().getBlobContainerClient(CONTAINER);
        try {
            var form=new SolicitacaoMatriculaFormDto();
            var a=form.getAluno();
            a.setNomeCompleto("Aluno fictício"); a.setDataNascimento(java.time.LocalDate.of(2023,1,15));
            a.setCpf("52998224725"); a.setCertidaoNascimento("123456"); a.setAutorizadosBuscar("Maria");
            var r=form.getResponsavel();
            r.setNomeCompleto("Maria"); r.setParentesco("Mãe"); r.setCpf("11144477735");
            r.setEmail("maria@example.com"); r.setTelefone("71999999999");
            r.setCep("40020000"); r.setLogradouro("Rua Teste"); r.setNumero("10"); r.setBairro("Centro"); r.setCidade("Salvador"); r.setEstado("BA");
            var c=form.getContatoEmergencia();
            c.setNome("João"); c.setParentesco("Tio"); c.setTelefone("71988888888");
            ImagensTeste.anexar(form);
            var s=solicitacoes.enviar(form);
            var documentos=solicitacoes.detalhar(s.getId()).getDocumentos();
            assertThat(documentos).hasSize(4);
            assertThat(client.listBlobs().stream().count()).isEqualTo(4);
            for (var documento:documentos) {
                assertThat(client.getBlobClient(documento.getChaveBlob()).downloadContent().toBytes())
                    .isEqualTo(ImagensTeste.bytes("png"));
                mvc.perform(get("/secretaria/solicitacoes/"+s.getId()+"/documentos/"+documento.getId())
                    .with(user("secretaria").roles("SECRETARIA")))
                    .andExpect(status().isOk()).andExpect(content().bytes(ImagensTeste.bytes("png")));
            }
        } finally { repository.deleteAll(); client.deleteIfExists(); }
    }
}
