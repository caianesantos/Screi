package com.escola.screi.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String limite(RedirectAttributes flash) {
        flash.addFlashAttribute("erroUpload","O envio excedeu o limite. Use até 5 MiB por imagem e 21 MiB no total. Selecione os arquivos novamente.");
        return "redirect:/responsavel/novo";
    }
}
