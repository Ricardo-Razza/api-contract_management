package com.contract_management.api.common.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.common.model.Ativo;
import com.contract_management.api.common.service.AtivoService;

@RestController
@RequestMapping("/ativos")
@RequiredArgsConstructor
public class AtivoController {

    private final AtivoService ativoService;

    @GetMapping
    public ResponseEntity<List<Ativo>> listarTodos() {
        return ResponseEntity.ok(ativoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ativo> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ativoService.buscarPorId(id));
    }

    @GetMapping("/situacao/{situacao}")
    public ResponseEntity<Ativo> buscarPorSituacao(@PathVariable String situacao) {
        return ResponseEntity.ok(ativoService.buscarPorSituacao(situacao));
    }
}