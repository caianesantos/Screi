package com.escola.screi;

import com.escola.screi.service.ViaCepService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepServiceTests {
    @Test void consultaEnderecoETrataCepInexistente() {
        var customizer=new MockServerRestTemplateCustomizer();
        var service=new ViaCepService(new RestTemplateBuilder(customizer));
        customizer.getServer().expect(requestTo("https://viacep.com.br/ws/40020000/json/"))
            .andRespond(withSuccess("{\"cep\":\"40020-000\",\"logradouro\":\"Rua Teste\",\"bairro\":\"Centro\",\"localidade\":\"Salvador\",\"uf\":\"BA\"}",MediaType.APPLICATION_JSON));
        assertThat(service.consultar("40020000").localidade()).isEqualTo("Salvador");
        customizer.getServer().verify();
        customizer.getServer().reset();
        customizer.getServer().expect(requestTo("https://viacep.com.br/ws/00000000/json/"))
            .andRespond(withSuccess("{\"erro\":true}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.consultar("00000000")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.consultar("abc")).isInstanceOf(IllegalArgumentException.class);
        customizer.getServer().verify();
    }
}
