package com.escola.screi.service;

import com.escola.screi.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public CustomUserDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    @Override public UserDetails loadUserByUsername(String email) {
        var usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
        return User.withUsername(usuario.getEmail()).password(usuario.getSenha()).authorities(usuario.getPapel().name()).build();
    }
}
