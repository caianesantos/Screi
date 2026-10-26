package com.escola.screi.repository;

import com.escola.screi.model.DocumentoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentoMatriculaRepository extends JpaRepository<DocumentoMatricula, Long> {
    Optional<DocumentoMatricula> findByIdAndSolicitacaoId(Long id, Long solicitacaoId);
}
