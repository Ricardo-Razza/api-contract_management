package com.contract_management.api.controller;

import com.contract_management.api.dto.response.FuncaoEquipeResponseDTO;
import com.contract_management.api.service.FuncaoEquipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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