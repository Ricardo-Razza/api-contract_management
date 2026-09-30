package com.contract_management.api.modules.ferias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.ferias.dto.request.PeriodoAquisitivoRequestDTO;
import com.contract_management.api.modules.ferias.dto.response.PeriodoAquisitivoResponseDTO;
import com.contract_management.api.modules.ferias.service.PeriodoAquisitivoService;

@RestController
@RequestMapping("/periodos-aquisitivos")
@RequiredArgsConstructor
@Tag(name = "Períodos Aquisitivos", description = "Endpoints para gerenciamento dos períodos aquisitivos de férias dos servidores")
public class PeriodoAquisitivoController {

    private final PeriodoAquisitivoService periodoService;

    @GetMapping
    public ResponseEntity<List<PeriodoAquisitivoResponseDTO>> listar() {
        return ResponseEntity.ok(periodoService.listar());
    }

    @GetMapping("/servidor/{servidorId}")
    @Operation(summary = "Listar períodos aquisitivos de um servidor")
    public ResponseEntity<List<PeriodoAquisitivoResponseDTO>> listarPorServidor(@PathVariable Long servidorId) {
        return ResponseEntity.ok(periodoService.listarPorServidor(servidorId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar período aquisitivo por ID")
    public ResponseEntity<PeriodoAquisitivoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(periodoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Criar novo período aquisitivo")
    public ResponseEntity<PeriodoAquisitivoResponseDTO> criar(@Valid @RequestBody PeriodoAquisitivoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodoService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar período aquisitivo existente")
    public ResponseEntity<PeriodoAquisitivoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PeriodoAquisitivoRequestDTO dto) {
        return ResponseEntity.ok(periodoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir período aquisitivo")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        periodoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}