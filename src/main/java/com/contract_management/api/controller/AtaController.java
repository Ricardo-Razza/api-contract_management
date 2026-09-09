package com.contract_management.api.controller;

import com.contract_management.api.dto.request.AtaRequestDTO;
import com.contract_management.api.dto.response.AtaResponseDTO;
import com.contract_management.api.service.AtaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/atas")
@RequiredArgsConstructor
public class AtaController {

    private final AtaService ataService;

    @GetMapping
    public ResponseEntity<List<AtaResponseDTO>> listarTodos() {
        return ResponseEntity.ok(ataService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ataService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<AtaResponseDTO> criar(@Valid @RequestBody AtaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ataService.criar(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        ataService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<AtaResponseDTO> atualizar(@PathVariable Long id, @RequestBody AtaRequestDTO dto) {
        return ResponseEntity.ok(ataService.atualizar(id, dto));
    }
}