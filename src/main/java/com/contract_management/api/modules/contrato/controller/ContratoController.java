package com.contract_management.api.modules.contrato.controller;

import com.contract_management.api.modules.contrato.dto.request.DocumentoFiltro;
import com.contract_management.api.modules.contrato.dto.response.DocumentoFiltrosDTO;
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

import com.contract_management.api.modules.contrato.dto.request.ContratoRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.contrato.service.ContratoService;

@RestController
@RequestMapping("/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @GetMapping
    public ResponseEntity<List<ContratoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(contratoService.listarTodos());
    }

    @GetMapping("/filtros")
    public ResponseEntity<DocumentoFiltrosDTO> filtrosDisponiveis() {
        return ResponseEntity.ok(contratoService.filtrosDisponiveis());
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<ContratoResponseDTO>> listarPaginado(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @ModelAttribute DocumentoFiltro filtro) {
        return ResponseEntity.ok(contratoService.listarPaginado(pageable, filtro));
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