package com.escola.screi;

import com.escola.screi.service.*;
import com.escola.screi.model.enums.TipoDocumento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

class ImagemDocumentoValidatorTests {
    private final ImagemDocumentoValidator validator=new ImagemDocumentoValidator();
    @ParameterizedTest @ValueSource(strings={"png","jpeg"})
    void aceitaImagensReaisERemoveConteudoAcrescentado(String formato) {
        byte[] original=ImagensTeste.bytes(formato);
        byte[] comConteudoExtra=java.util.Arrays.copyOf(original,original.length+30);
        byte[] extra="<script>alert(1)</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(extra,0,comConteudoExtra,original.length,extra.length);
        var arquivo=new MockMultipartFile("arquivo","documento."+formato,"application/octet-stream",comConteudoExtra);
        var imagem=validator.validar(TipoDocumento.CPF_ALUNO,arquivo);
        assertThat(imagem.contentType()).isEqualTo("image/"+formato);
        assertThat(new String(imagem.dados(),java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("<script>");
        assertThat(imagem.dados()).isNotEmpty();
        if (formato.equals("png")) assertThat(imagem.dados()).isEqualTo(original);
    }
    @Test void bloqueiaArquivosVaziosFalsosFormatosEExtensoes() {
        for (var arquivo:java.util.List.of(
            new MockMultipartFile("arquivo","doc.png","image/png",new byte[0]),
            new MockMultipartFile("arquivo","doc.jpg","image/jpeg","<svg>não é imagem</svg>".getBytes()),
            new MockMultipartFile("arquivo","doc.svg","image/png",ImagensTeste.bytes("png")),
            new MockMultipartFile("arquivo","doc.jpg","image/jpeg",ImagensTeste.bytes("png")))) {
            assertThatThrownBy(() -> validator.validar(TipoDocumento.CPF_ALUNO,arquivo)).isInstanceOf(DocumentoInvalidoException.class);
        }
        assertThatThrownBy(() -> validator.validar(TipoDocumento.CPF_ALUNO,null)).isInstanceOf(DocumentoInvalidoException.class);
    }
    @Test void bloqueiaArquivoMaiorQueCincoMiB() {
        var arquivo=new MockMultipartFile("arquivo","doc.png","image/png",new byte[ImagemDocumentoValidator.MAX_BYTES+1]);
        assertThatThrownBy(() -> validator.validar(TipoDocumento.CPF_ALUNO,arquivo))
            .isInstanceOf(DocumentoInvalidoException.class).hasMessageContaining("5 MiB");
    }
    @Test void bloqueiaDimensoesExcessivasAntesDeDecodificarPixels() throws Exception {
        var imagem=new java.awt.image.BufferedImage(10001,1,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagem,"png",output);
        var arquivo=new MockMultipartFile("arquivo","doc.png","image/png",output.toByteArray());
        assertThatThrownBy(() -> validator.validar(TipoDocumento.CPF_ALUNO,arquivo))
            .isInstanceOf(DocumentoInvalidoException.class).hasMessageContaining("megapixels");
    }
}
