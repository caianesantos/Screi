package com.escola.screi.dto;

import com.escola.screi.model.enums.TipoDocumento;

/** Resultado de validação independente da implementação do validador. */
public record DocumentoValidado(TipoDocumento tipo, byte[] dados, String contentType, String extensao) {}
