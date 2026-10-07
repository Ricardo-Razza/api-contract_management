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

import com.contract_management.api.modules.contrato.dto.request.AtaRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.service.AtaService;

@RestController
@RequestMapping("/atas")
@RequiredArgsConstructor
public class AtaController {

    private final AtaService ataService;

    @GetMapping
    public ResponseEntity<List<AtaResponseDTO>> listarTodos() {
        return ResponseEntity.ok(ataService.listarTodos());
    }

    @GetMapping("/filtros")
    public ResponseEntity<DocumentoFiltrosDTO> filtrosDisponiveis() {
        return ResponseEntity.ok(ataService.filtrosDisponiveis());
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<AtaResponseDTO>> listarPaginado(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @ModelAttribute DocumentoFiltro filtro) {
        return ResponseEntity.ok(ataService.listarPaginado(pageable, filtro));
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
    public ResponseEntity<AtaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody AtaRequestDTO dto) {
        return ResponseEntity.ok(ataService.atualizar(id, dto));
    }
}