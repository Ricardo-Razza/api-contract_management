package com.contract_management.api.controller;

import com.contract_management.api.dto.request.AlterarPapelRequest;
import com.contract_management.api.dto.request.UsuarioRequest;
import com.contract_management.api.dto.response.UsuarioResponse;
import com.contract_management.api.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    @PatchMapping("/{id}/papel")
    public ResponseEntity<UsuarioResponse> alterarPapel(
            @PathVariable Long id,
            @Valid @RequestBody AlterarPapelRequest request
    ) {
        return ResponseEntity.ok(usuarioService.alterarPapel(id, request));
    }
}
