package com.escola.screi.service;

import com.escola.screi.dto.DocumentoValidado;
import com.escola.screi.model.enums.TipoDocumento;
import org.springframework.web.multipart.MultipartFile;

/** Valida e prepara os bytes; não realiza upload nem altera a solicitação. */
public interface ValidadorDocumento {
    DocumentoValidado validar(TipoDocumento tipo, MultipartFile arquivo);
}
