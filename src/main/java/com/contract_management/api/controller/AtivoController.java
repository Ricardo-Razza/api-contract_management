package com.contract_management.api.controller;

import com.contract_management.api.model.Ativo;
import com.contract_management.api.service.AtivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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