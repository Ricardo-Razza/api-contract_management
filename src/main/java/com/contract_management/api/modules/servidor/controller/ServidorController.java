package com.contract_management.api.modules.servidor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.servidor.dto.request.ServidorRequestDTO;
import com.contract_management.api.modules.servidor.dto.response.ServidorResponseDTO;
import com.contract_management.api.modules.servidor.service.ServidorService;

@RestController
@RequestMapping("/servidores")
@RequiredArgsConstructor
public class ServidorController {

    private final ServidorService servidorService;

    @GetMapping
    public ResponseEntity<List<ServidorResponseDTO>> listarTodos() {
        return ResponseEntity.ok(servidorService.listarTodos());
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<ServidorResponseDTO>> listarPaginado(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(servidorService.listarPaginado(pageable));
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