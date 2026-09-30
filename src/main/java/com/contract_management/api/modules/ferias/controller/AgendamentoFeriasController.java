package com.contract_management.api.modules.ferias.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.ferias.dto.request.AgendamentoFeriasRequestDTO;
import com.contract_management.api.modules.ferias.dto.request.VerificacaoConflitoRequestDTO;
import com.contract_management.api.modules.ferias.dto.response.AgendamentoFeriasResponseDTO;
import com.contract_management.api.modules.ferias.dto.response.EscalaAnualDTO;
import com.contract_management.api.modules.ferias.dto.response.VerificacaoConflitoResponseDTO;
import com.contract_management.api.modules.ferias.service.AgendamentoFeriasService;
import com.contract_management.api.modules.ferias.service.EscalaFeriasService;

@RestController
@RequestMapping("/ferias")
@RequiredArgsConstructor
@Tag(name = "Férias e Afastamentos", description = "Endpoints para agendamento de férias, verificação de conflitos e escala anual")
public class AgendamentoFeriasController {

    private final AgendamentoFeriasService agendamentoService;
    private final EscalaFeriasService escalaFeriasService;

    @GetMapping
    public ResponseEntity<List<AgendamentoFeriasResponseDTO>> listar(@RequestParam(required = false) Integer ano) {
        return ResponseEntity.ok(agendamentoService.listar(ano));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoFeriasResponseDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.cancelar(id));
    }

    @GetMapping("/escala-anual")
    @Operation(summary = "Gerar matriz de escala anual de férias por ano, secretaria e setor")
    public ResponseEntity<EscalaAnualDTO> obterEscalaAnual(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Long secretariaId,
            @RequestParam(required = false) String setor) {
        return ResponseEntity.ok(escalaFeriasService.gerarEscalaAnual(ano, secretariaId, setor));
    }

    @PostMapping("/verificar-conflito")
    @Operation(summary = "Verificar em tempo real se há sobreposição/conflito com outros servidores")
    public ResponseEntity<VerificacaoConflitoResponseDTO> verificarConflito(
            @Valid @RequestBody VerificacaoConflitoRequestDTO dto) {
        return ResponseEntity.ok(agendamentoService.verificarConflitos(dto));
    }

    @GetMapping("/servidor/{servidorId}")
    @Operation(summary = "Listar todos os agendamentos de um servidor")
    public ResponseEntity<List<AgendamentoFeriasResponseDTO>> listarPorServidor(@PathVariable Long servidorId) {
        return ResponseEntity.ok(agendamentoService.listarPorServidor(servidorId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID")
    public ResponseEntity<AgendamentoFeriasResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Criar novo agendamento de férias ou afastamento")
    public ResponseEntity<AgendamentoFeriasResponseDTO> criar(@Valid @RequestBody AgendamentoFeriasRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agendamento existente")
    public ResponseEntity<AgendamentoFeriasResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AgendamentoFeriasRequestDTO dto) {
        return ResponseEntity.ok(agendamentoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir agendamento e estornar saldo do período aquisitivo")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        agendamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}