package com.contract_management.api.modules.equipe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.equipe.dto.request.EquipeContratoRequestDTO;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.equipe.service.EquipeContratoService;

@RestController
@RequestMapping("/equipes-contrato")
@RequiredArgsConstructor
public class EquipeContratoController {

    private final EquipeContratoService equipeContratoService;

    @GetMapping
    public ResponseEntity<List<EquipeContratoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(equipeContratoService.buscarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeContratoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(equipeContratoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EquipeContratoResponseDTO> criar(@Valid @RequestBody EquipeContratoRequestDTO requestDTO) {
        EquipeContratoResponseDTO criada = equipeContratoService.salvar(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipeContratoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody EquipeContratoRequestDTO requestDTO) {
        return ResponseEntity.ok(equipeContratoService.atualizar(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        equipeContratoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}