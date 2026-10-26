package com.escola.screi;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

final class ImagensTeste {
    private ImagensTeste() {}
    static byte[] bytes(String formato) {
        try {
            var image=new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB);
            image.setRGB(1,1,0x00aaff);
            var output=new ByteArrayOutputStream();
            ImageIO.write(image,formato,output);
            return output.toByteArray();
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    static MockMultipartFile arquivo(String campo) {
        return new MockMultipartFile(campo,"documento.png","image/png",bytes("png"));
    }
    static MockMultipartHttpServletRequestBuilder anexar(MockMultipartHttpServletRequestBuilder request) {
        return request.file(arquivo("cpfAlunoImagem")).file(arquivo("certidaoNascimentoImagem"))
            .file(arquivo("cpfResponsavelImagem")).file(arquivo("comprovanteResidenciaImagem"));
    }
    static void anexar(com.escola.screi.dto.SolicitacaoMatriculaFormDto form) {
        form.setCpfAlunoImagem(arquivo("cpfAlunoImagem"));
        form.setCertidaoNascimentoImagem(arquivo("certidaoNascimentoImagem"));
        form.setCpfResponsavelImagem(arquivo("cpfResponsavelImagem"));
        form.setComprovanteResidenciaImagem(arquivo("comprovanteResidenciaImagem"));
    }
}
