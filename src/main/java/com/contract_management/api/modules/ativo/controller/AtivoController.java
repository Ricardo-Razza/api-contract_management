package com.contract_management.api.modules.ativo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.ativo.dto.response.AtivoResponseDTO;
import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.ativo.service.AtivoService;

@RestController
@RequestMapping("/ativos")
@RequiredArgsConstructor
public class AtivoController {

    private final AtivoService ativoService;

    @GetMapping
    public ResponseEntity<List<AtivoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(ativoService.listarTodos().stream()
                .map(this::toDTO)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtivoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(ativoService.buscarPorId(id)));
    }

    @GetMapping("/situacao/{situacao}")
    public ResponseEntity<AtivoResponseDTO> buscarPorSituacao(@PathVariable String situacao) {
        return ResponseEntity.ok(toDTO(ativoService.buscarPorSituacao(situacao)));
    }

    private AtivoResponseDTO toDTO(Ativo ativo) {
        if (ativo == null) return null;
        return AtivoResponseDTO.builder()
                .id(ativo.getId())
                .situacao(ativo.getSituacao())
                .build();
    }
}