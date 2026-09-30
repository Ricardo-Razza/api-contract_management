package com.contract_management.api.modules.equipe.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.equipe.dto.response.FuncaoEquipeResponseDTO;
import com.contract_management.api.modules.equipe.service.FuncaoEquipeService;

@RestController
@RequestMapping("/funcoes-equipe")
@RequiredArgsConstructor
public class FuncaoEquipeController {

    private final FuncaoEquipeService funcaoEquipeService;

    @GetMapping
    public ResponseEntity<List<FuncaoEquipeResponseDTO>> listarTodos() {
        return ResponseEntity.ok(funcaoEquipeService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncaoEquipeResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(funcaoEquipeService.buscarPorId(id));
    }
}