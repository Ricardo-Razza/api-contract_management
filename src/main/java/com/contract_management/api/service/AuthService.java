package com.contract_management.api.service;

import com.contract_management.api.dto.request.LoginRequest;
import com.contract_management.api.dto.response.LoginResponse;
import com.contract_management.api.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        Usuario usuario = (Usuario) usuarioDetailsService.loadUserByUsername(request.email());
        return new LoginResponse(
                jwtService.generateToken(usuario),
                "Bearer",
                jwtService.getExpirationMs(),
                usuario.getPapel().name()
        );
    }
}
