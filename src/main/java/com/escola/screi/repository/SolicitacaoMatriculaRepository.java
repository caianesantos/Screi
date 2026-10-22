package com.escola.screi.repository;

import com.escola.screi.model.SolicitacaoMatricula;
import com.escola.screi.model.enums.StatusMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SolicitacaoMatriculaRepository extends JpaRepository<SolicitacaoMatricula, Long> {
    List<SolicitacaoMatricula> findByProtocoloIgnoreCase(String protocolo);
    List<SolicitacaoMatricula> findDistinctByResponsaveisCpfOrderByDataSubmissaoDesc(String cpf);
    List<SolicitacaoMatricula> findByStatusOrderByDataSubmissaoDesc(StatusMatricula status);
    List<SolicitacaoMatricula> findAllByOrderByDataSubmissaoDesc();
}
