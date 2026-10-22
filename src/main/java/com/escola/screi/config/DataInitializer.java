package com.escola.screi.config;

import com.escola.screi.model.Usuario;
import com.escola.screi.model.enums.TipoPapel;
import com.escola.screi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DataInitializer implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;
    private final String email;
    private final String senha;
    public DataInitializer(UsuarioRepository usuarios, BCryptPasswordEncoder encoder,
        @Value("${screi.secretaria.email}") String email, @Value("${screi.secretaria.senha}") String senha) {
        this.usuarios=usuarios; this.encoder=encoder; this.email=email; this.senha=senha;
    }
    @Override public void run(String... args) {
        if (usuarios.findByEmailIgnoreCase(email).isEmpty()) {
            Usuario usuario=new Usuario(); usuario.setNome("Secretaria escolar"); usuario.setEmail(email);
            usuario.setSenha(encoder.encode(senha)); usuario.setPapel(TipoPapel.ROLE_SECRETARIA); usuarios.save(usuario);
        }
    }
}
