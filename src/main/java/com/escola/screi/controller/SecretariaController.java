package com.escola.screi.controller;

import com.escola.screi.dto.StatusUpdateDto;
import com.escola.screi.model.enums.StatusMatricula;
import com.escola.screi.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import java.io.IOException;
@Controller @RequestMapping("/secretaria")
public class SecretariaController {
    private final SolicitacaoService solicitacoes;
    private final ExcelExportService excel;
    public SecretariaController(SolicitacaoService solicitacoes, ExcelExportService excel) { this.solicitacoes=solicitacoes; this.excel=excel; }
    @GetMapping({"", "/triagem"}) public String triagem(Model model) { model.addAttribute("solicitacoes",solicitacoes.listar()); return "triagem"; }
    @GetMapping("/solicitacoes/{id}") public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("solicitacao",solicitacoes.detalhar(id)); model.addAttribute("statusDisponiveis",StatusMatricula.values());
        if (!model.containsAttribute("atualizacao")) model.addAttribute("atualizacao",new StatusUpdateDto()); return "detalhes";
    }
    @PostMapping("/solicitacoes/{id}/status") public String atualizar(@PathVariable Long id,
        @Valid @ModelAttribute("atualizacao") StatusUpdateDto dto, BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) return detalhes(id,model);
        try { solicitacoes.atualizarStatus(id,dto); }
        catch (IllegalArgumentException ex) { result.reject("status",ex.getMessage()); return detalhes(id,model); }
        flash.addFlashAttribute("sucesso","Situação atualizada."); return "redirect:/secretaria/solicitacoes/"+id;
    }
    @GetMapping("/exportar-excel") public ResponseEntity<byte[]> exportar() throws IOException {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=matriculas-aprovadas.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excel.exportar());
    }
}
