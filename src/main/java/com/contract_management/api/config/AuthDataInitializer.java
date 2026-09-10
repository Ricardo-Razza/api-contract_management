package com.contract_management.api.config;

import com.contract_management.api.model.Papel;
import com.contract_management.api.model.Usuario;
import com.contract_management.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AuthDataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDefaultUsers(
            @Value("${auth.seed-default-users}") boolean seedDefaultUsers,
            @Value("${auth.admin.name}") String adminName,
            @Value("${auth.admin.email}") String adminEmail,
            @Value("${auth.admin.password}") String adminPassword,
            @Value("${auth.gestor.name}") String gestorName,
            @Value("${auth.gestor.email}") String gestorEmail,
            @Value("${auth.gestor.password}") String gestorPassword
    ) {
        return args -> {
            if (!seedDefaultUsers) {
                return;
            }
            createIfAbsent(adminName, adminEmail, adminPassword, Papel.ADMIN);
            createIfAbsent(gestorName, gestorEmail, gestorPassword, Papel.GESTOR);
        };
    }

    private void createIfAbsent(String nome, String email, String senha, Papel papel) {
        if (!usuarioRepository.existsByEmailIgnoreCase(email)) {
            Usuario usuario = new Usuario();
            usuario.setNome(nome);
            usuario.setEmail(email);
            usuario.setSenha(passwordEncoder.encode(senha));
            usuario.setPapel(papel);
            usuarioRepository.save(usuario);
        }
    }
}
