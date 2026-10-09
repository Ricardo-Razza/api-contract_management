package com.contract_management.api.modules.contrato.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.contrato.dto.response.TipoResponseDTO;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.service.TipoService;

@RestController
@RequestMapping("/tipos")
@RequiredArgsConstructor
public class TipoController {

    private final TipoService tipoService;

    @GetMapping
    public ResponseEntity<List<TipoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(tipoService.listarTodos().stream()
                .map(this::toDTO)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(toDTO(tipoService.buscarPorId(id)));
    }

    @GetMapping("/nome/{tipoArp}")
    public ResponseEntity<TipoResponseDTO> buscarPorTipoArp(@PathVariable String tipoArp) {
        return ResponseEntity.ok(toDTO(tipoService.buscarPorTipoArp(tipoArp)));
    }

    private TipoResponseDTO toDTO(Tipo tipo) {
        if (tipo == null) return null;
        return TipoResponseDTO.builder()
                .id(tipo.getId())
                .tipoArp(tipo.getTipoArp())
                .build();
    }
}