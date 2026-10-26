package com.escola.screi.service;

import com.escola.screi.dto.SolicitacaoMatriculaFormDto;
import com.escola.screi.dto.DocumentoValidado;
import com.escola.screi.model.*;
import com.escola.screi.model.enums.TipoDocumento;
import com.escola.screi.repository.DocumentoMatriculaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.slf4j.LoggerFactory;
import java.util.*;

@Service
public class DocumentoService {
    private final DocumentoStorage storage;
    private final ValidadorDocumento validator;
    private final DocumentoMatriculaRepository repository;
    public DocumentoService(DocumentoStorage storage, ValidadorDocumento validator, DocumentoMatriculaRepository repository) {
        this.storage=storage; this.validator=validator; this.repository=repository;
    }
    public List<DocumentoValidado> validar(SolicitacaoMatriculaFormDto form) {
        return Arrays.stream(TipoDocumento.values()).map(tipo -> validator.validar(tipo,form.arquivo(tipo))).toList();
    }
    public void anexar(SolicitacaoMatricula solicitacao, List<DocumentoValidado> imagens) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("Uma transação é necessária para anexar documentos.");
        List<String> chaves=new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status==STATUS_COMMITTED) return;
                for (String chave:chaves) {
                    try { storage.excluir(chave); }
                    catch (RuntimeException ex) {
                        LoggerFactory.getLogger(DocumentoService.class).warn("Falha ao remover imagem de cadastro não concluído: {}",chave);
                    }
                }
            }
        });
        for (var imagem:imagens) {
            String chave=solicitacao.getProtocolo()+"/"+UUID.randomUUID()+"."+imagem.extensao();
            chaves.add(chave);
            storage.enviar(chave,imagem.dados(),imagem.contentType());
            var documento=new DocumentoMatricula();
            documento.setSolicitacao(solicitacao); documento.setTipo(imagem.tipo()); documento.setChaveBlob(chave);
            documento.setContentType(imagem.contentType()); documento.setTamanho(imagem.dados().length);
            solicitacao.getDocumentos().add(documento);
        }
    }
    @Transactional(readOnly=true)
    public ImagemDocumento obter(Long solicitacaoId, Long documentoId) {
        var documento=repository.findByIdAndSolicitacaoId(documentoId,solicitacaoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));
        String extensao=documento.getContentType().equals("image/png") ? "png" : "jpg";
        return new ImagemDocumento(storage.ler(documento.getChaveBlob()),documento.getContentType(),documento.getTipo().name().toLowerCase(Locale.ROOT)+"."+extensao);
    }
    public record ImagemDocumento(byte[] dados, String contentType, String nome) {}
}
