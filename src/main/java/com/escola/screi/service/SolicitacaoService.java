package com.escola.screi.service;

import com.escola.screi.dto.*;
import com.escola.screi.model.*;
import com.escola.screi.model.enums.StatusMatricula;
import com.escola.screi.repository.SolicitacaoMatriculaRepository;
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
    public SolicitacaoService(SolicitacaoMatriculaRepository repository) { this.repository=repository; }
    @Transactional
    public SolicitacaoMatricula enviar(@Valid SolicitacaoMatriculaFormDto form) {
        SolicitacaoMatricula s=new SolicitacaoMatricula();
        form.getAluno().setId(null); form.getResponsavel().setId(null); form.getContatoEmergencia().setId(null);
        form.getAluno().setCpf(form.getAluno().getCpf().replaceAll("\\D", ""));
        form.getResponsavel().setCpf(form.getResponsavel().getCpf().replaceAll("\\D", ""));
        s.setAluno(form.getAluno()); s.getResponsaveis().add(form.getResponsavel());
        s.getContatosEmergencia().add(form.getContatoEmergencia()); s.setStatus(StatusMatricula.ENVIADO);
        return repository.save(s);
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
