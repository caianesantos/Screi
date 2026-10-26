package com.escola.screi.controller;

import com.escola.screi.service.RecursoNaoEncontradoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class RecursoExceptionHandler {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Void> naoEncontrado() { return ResponseEntity.notFound().build(); }
}
