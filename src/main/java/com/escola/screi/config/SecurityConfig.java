package com.escola.screi.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
    @Bean public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/responsavel/**", "/css/**", "/js/**", "/error").permitAll()
            .requestMatchers("/secretaria/**").hasRole("SECRETARIA")
            .anyRequest().authenticated())
            .formLogin(login -> login.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/secretaria/triagem", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .build();
    }
}
