package com.contract_management.api.controller;

import com.contract_management.api.dto.request.SecretariaRequestDTO;
import com.contract_management.api.dto.response.SecretariaResponseDTO;
import com.contract_management.api.service.SecretariaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/secretarias")
@RequiredArgsConstructor
public class SecretariaController {

    private final SecretariaService secretariaService;

    @GetMapping
    public ResponseEntity<List<SecretariaResponseDTO>> listarTodos() {
        return ResponseEntity.ok(secretariaService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecretariaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(secretariaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<SecretariaResponseDTO> criar(@Valid @RequestBody SecretariaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(secretariaService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SecretariaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody SecretariaRequestDTO dto) {
        return ResponseEntity.ok(secretariaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        secretariaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}