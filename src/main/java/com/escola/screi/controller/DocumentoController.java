package com.escola.screi.controller;

import com.escola.screi.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/secretaria/solicitacoes/{solicitacaoId}/documentos")
public class DocumentoController {
    private final DocumentoService documentos;
    public DocumentoController(DocumentoService documentos) { this.documentos=documentos; }
    @GetMapping("/{documentoId}") public ResponseEntity<byte[]> obter(@PathVariable Long solicitacaoId, @PathVariable Long documentoId) {
        var imagem=documentos.obter(solicitacaoId,documentoId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(imagem.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(imagem.nome()).build().toString())
            .body(imagem.dados());
    }
    @ExceptionHandler(ArmazenamentoIndisponivelException.class)
    ResponseEntity<String> indisponivel() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(CacheControl.noStore())
            .body("A imagem está temporariamente indisponível. Tente novamente.");
    }
}
