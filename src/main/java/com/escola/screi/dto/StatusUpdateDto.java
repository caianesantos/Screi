package com.escola.screi.dto;

import com.escola.screi.model.enums.StatusMatricula;
import jakarta.validation.constraints.*;


public class StatusUpdateDto {
    @NotNull
    private StatusMatricula status;
    @Size(max=2000)
    private String justificativaReprovacao;
    public StatusMatricula getStatus() { return status; }
    public void setStatus(StatusMatricula status) { this.status = status; }
    public String getJustificativaReprovacao() { return justificativaReprovacao; }
    public void setJustificativaReprovacao(String justificativaReprovacao) { this.justificativaReprovacao = justificativaReprovacao; }

    @AssertTrue(message="Informe a justificativa para registrar uma pendência.")
    public boolean isJustificativaValida() {
        return status != StatusMatricula.COM_PENDENCIA ||
            (justificativaReprovacao != null && !justificativaReprovacao.isBlank());
    }

}
