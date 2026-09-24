package com.contract_management.api.controller;

import com.contract_management.api.dto.request.LocalInstalacaoRequestDTO;
import com.contract_management.api.dto.response.LocalInstalacaoResponseDTO;
import com.contract_management.api.service.LocalInstalacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locais-instalacao")
@RequiredArgsConstructor
@Tag(name = "Locais de Instalação", description = "CRUD e Gestão de Prédios, Unidades e Setores de Instalação")
public class LocalInstalacaoController {

    private final LocalInstalacaoService localService;

    @GetMapping
    @Operation(summary = "Lista todos os locais de instalação ativos com opção de filtro por secretaria")
    public ResponseEntity<List<LocalInstalacaoResponseDTO>> listarTodos(@RequestParam(required = false) Long secretariaId) {
        return ResponseEntity.ok(localService.listarTodos(secretariaId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de um local de instalação por ID")
    public ResponseEntity<LocalInstalacaoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(localService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo local de instalação")
    public ResponseEntity<LocalInstalacaoResponseDTO> criar(@Valid @RequestBody LocalInstalacaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(localService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados de um local de instalação existente")
    public ResponseEntity<LocalInstalacaoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody LocalInstalacaoRequestDTO dto) {
        return ResponseEntity.ok(localService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa um local de instalação")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        localService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sincronizar")
    @Operation(summary = "Sincroniza locais a partir das instalações de impressoras")
    public ResponseEntity<List<LocalInstalacaoResponseDTO>> sincronizar() {
        localService.sincronizarLocaisDasInstalacoes();
        return ResponseEntity.ok(localService.listarTodos(null));
    }
}
