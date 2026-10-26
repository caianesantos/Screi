package com.escola.screi.repository;

import com.escola.screi.model.SolicitacaoMatricula;
import com.escola.screi.service.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.Locale;

/** Adapta os detalhes de persistência à operação esperada pela aplicação. */
// O repositório Spring Data já traduz falhas JPA; preservar aqui a exceção de negócio.
@Component
public class JpaRegistroSolicitacao implements RegistroSolicitacao {
    private final SolicitacaoMatriculaRepository repository;
    public JpaRegistroSolicitacao(SolicitacaoMatriculaRepository repository) { this.repository=repository; }
    @Override public SolicitacaoMatricula salvar(SolicitacaoMatricula solicitacao) {
        try {
            return repository.saveAndFlush(solicitacao);
        } catch (DataIntegrityViolationException ex) {
            // A restrição também protege contra envios concorrentes do mesmo CPF.
            for (Throwable causa=ex; causa!=null; causa=causa.getCause()) {
                if (causa instanceof org.hibernate.exception.ConstraintViolationException violacao &&
                    violacao.getConstraintName()!=null &&
                    violacao.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_aluno_cpf")) {
                    throw new AlunoJaCadastradoException();
                }
            }
            throw ex;
        }
    }
}
