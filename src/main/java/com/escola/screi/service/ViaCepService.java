package com.escola.screi.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
@Service
public class ViaCepService {
    private final RestTemplate restTemplate;
    public ViaCepService(RestTemplateBuilder builder) {
        restTemplate=builder.connectTimeout(Duration.ofSeconds(3)).readTimeout(Duration.ofSeconds(5)).build();
    }
    public Endereco consultar(String cep) {
        if (cep==null || !cep.matches("[0-9]{8}")) throw new IllegalArgumentException("Informe um CEP de 8 dígitos.");
        Endereco endereco=restTemplate.getForObject("https://viacep.com.br/ws/{cep}/json/",Endereco.class,cep);
        if (endereco==null || Boolean.TRUE.equals(endereco.erro())) throw new IllegalArgumentException("CEP não encontrado.");
        return endereco;
    }
    public record Endereco(String cep, String logradouro, String bairro, String localidade, String uf, Boolean erro) {}
}
