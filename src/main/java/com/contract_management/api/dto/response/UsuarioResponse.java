package com.contract_management.api.dto.response;

import com.contract_management.api.model.Papel;
import com.contract_management.api.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, Papel papel) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel());
    }
}
