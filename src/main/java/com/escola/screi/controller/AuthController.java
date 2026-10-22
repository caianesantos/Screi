package com.escola.screi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AuthController {
    @GetMapping("/") public String inicio() { return "redirect:/responsavel/novo"; }
    @GetMapping("/login") public String login() { return "login"; }
}
