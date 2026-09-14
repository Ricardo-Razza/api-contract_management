package com.contract_management.api.controller;

import com.contract_management.api.dto.request.ContratoRequestDTO;
import com.contract_management.api.dto.response.ContratoResponseDTO;
import com.contract_management.api.service.ContratoService;
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

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @GetMapping
    public ResponseEntity<List<ContratoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(contratoService.listarTodos());
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<ContratoResponseDTO>> listarPaginado(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contratoService.listarPaginado(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ContratoResponseDTO> criar(@Valid @RequestBody ContratoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratoService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContratoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ContratoRequestDTO dto) {
        return ResponseEntity.ok(contratoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        contratoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
