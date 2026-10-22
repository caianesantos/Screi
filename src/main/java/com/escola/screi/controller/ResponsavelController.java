package com.escola.screi.controller;

import com.escola.screi.dto.SolicitacaoMatriculaFormDto;
import com.escola.screi.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
@Controller @RequestMapping("/responsavel")
public class ResponsavelController {
    private final SolicitacaoService solicitacoes;
    private final ViaCepService viaCep;
    public ResponsavelController(SolicitacaoService solicitacoes, ViaCepService viaCep) { this.solicitacoes=solicitacoes; this.viaCep=viaCep; }
    @InitBinder("form") void vincular(WebDataBinder binder) {
        binder.setAllowedFields("aluno.nomeCompleto","aluno.dataNascimento","aluno.certidaoNascimento","aluno.cpf","aluno.autorizadosBuscar",
            "responsavel.nomeCompleto","responsavel.parentesco","responsavel.cpf","responsavel.telefone","responsavel.email","responsavel.cep",
            "responsavel.logradouro","responsavel.numero","responsavel.bairro","responsavel.cidade","responsavel.estado",
            "contatoEmergencia.nome","contatoEmergencia.parentesco","contatoEmergencia.telefone");
    }
    @GetMapping("/novo") public String novo(Model model) { model.addAttribute("form",new SolicitacaoMatriculaFormDto()); return "wizard"; }
    @PostMapping("/solicitacoes") public String enviar(@Valid @ModelAttribute("form") SolicitacaoMatriculaFormDto form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) return "wizard";
        var s=solicitacoes.enviar(form); flash.addFlashAttribute("sucesso","Solicitação enviada. Guarde seu protocolo: "+s.getProtocolo());
        return "redirect:/responsavel/status?consulta="+s.getProtocolo();
    }
    @GetMapping("/status") public String status(@RequestParam(defaultValue="") String consulta, Model model) {
        model.addAttribute("consulta",consulta); model.addAttribute("solicitacoes",solicitacoes.consultar(consulta)); return "status";
    }
    @GetMapping("/cep/{cep}") @ResponseBody public ResponseEntity<?> cep(@PathVariable String cep) {
        try { return ResponseEntity.ok(viaCep.consultar(cep)); }
        catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(java.util.Map.of("mensagem",ex.getMessage())); }
        catch (RestClientException ex) { return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(java.util.Map.of("mensagem","Consulta indisponível. Preencha o endereço manualmente.")); }
    }
}
