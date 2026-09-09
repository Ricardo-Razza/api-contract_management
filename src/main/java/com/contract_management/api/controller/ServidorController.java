package com.contract_management.api.controller;

import com.contract_management.api.dto.request.ServidorRequestDTO;
import com.contract_management.api.dto.response.ServidorResponseDTO;
import com.contract_management.api.service.ServidorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servidores")
@RequiredArgsConstructor
public class ServidorController {

    private final ServidorService servidorService;

    @GetMapping
    public ResponseEntity<List<ServidorResponseDTO>> listarTodos() {
        return ResponseEntity.ok(servidorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServidorResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servidorService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ServidorResponseDTO> criar(@Valid @RequestBody ServidorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servidorService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServidorResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ServidorRequestDTO dto) {
        return ResponseEntity.ok(servidorService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        servidorService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}