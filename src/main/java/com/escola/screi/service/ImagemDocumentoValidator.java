package com.escola.screi.service;

import com.escola.screi.model.enums.TipoDocumento;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.Locale;

@Component
public class ImagemDocumentoValidator {
    public static final int MAX_BYTES=5*1024*1024;
    private static final long MAX_PIXELS=16_000_000;
    public ImagemValidada validar(TipoDocumento tipo, MultipartFile arquivo) {
        if (arquivo==null || arquivo.isEmpty()) throw erro(tipo,"Adicione a imagem deste documento.");
        if (arquivo.getSize()>MAX_BYTES) throw erro(tipo,"A imagem deve ter no máximo 5 MiB.");
        String nome=arquivo.getOriginalFilename()==null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        String extensao=nome.endsWith(".png") ? "png" : (nome.endsWith(".jpg") || nome.endsWith(".jpeg") ? "jpeg" : "");
        if (extensao.isEmpty()) throw erro(tipo,"Envie uma imagem JPEG ou PNG.");
        try (var input=arquivo.getInputStream()) {
            byte[] original=input.readNBytes(MAX_BYTES+1);
            if (original.length>MAX_BYTES) throw erro(tipo,"A imagem deve ter no máximo 5 MiB.");
            try (var imageInput=ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
                var readers=ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw erro(tipo,"O arquivo não contém uma imagem válida.");
                var reader=readers.next();
                try {
                    reader.setInput(imageInput,true,true);
                    String formato=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!formato.equals(extensao)) throw erro(tipo,"O conteúdo da imagem não corresponde ao formato do arquivo.");
                    int largura=reader.getWidth(0), altura=reader.getHeight(0);
                    if (largura<=0 || altura<=0 || largura>10000 || altura>10000 || (long)largura*altura>MAX_PIXELS)
                        throw erro(tipo,"Use uma imagem de até 16 megapixels e 10.000 pixels por lado.");
                    var imagem=reader.read(0);
                    var output=new ByteArrayOutputStream();
                    // Re-encode actual pixels; discard source metadata and appended content.
                    if (imagem==null || !ImageIO.write(imagem,formato,output)) throw erro(tipo,"Não foi possível processar a imagem.");
                    if (output.size()>MAX_BYTES) throw erro(tipo,"A imagem processada excede 5 MiB. Reduza a resolução.");
                    return new ImagemValidada(tipo,output.toByteArray(),"image/"+formato,formato.equals("jpeg") ? "jpg" : "png");
                } finally { reader.dispose(); }
            }
        } catch (IOException ex) { throw erro(tipo,"Não foi possível ler a imagem. Selecione um arquivo JPEG ou PNG válido."); }
    }
    private DocumentoInvalidoException erro(TipoDocumento tipo, String mensagem) {
        return new DocumentoInvalidoException(tipo.getCampo(),mensagem);
    }
    public record ImagemValidada(TipoDocumento tipo, byte[] dados, String contentType, String extensao) {}
}
