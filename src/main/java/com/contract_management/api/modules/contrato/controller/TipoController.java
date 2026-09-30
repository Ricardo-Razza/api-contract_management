package com.contract_management.api.modules.contrato.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.service.TipoService;

@RestController
@RequestMapping("/tipos")
@RequiredArgsConstructor
public class TipoController {

    private final TipoService tipoService;

    @GetMapping
    public ResponseEntity<List<Tipo>> listarTodos() {
        return ResponseEntity.ok(tipoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tipo> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tipoService.buscarPorId(id));
    }

    @GetMapping("/nome/{tipoArp}")
    public ResponseEntity<Tipo> buscarPorTipoArp(@PathVariable String tipoArp) {
        return ResponseEntity.ok(tipoService.buscarPorTipoArp(tipoArp));
    }
}