package com.contract_management.api.controller;

import com.contract_management.api.dto.request.ImpressoraRequestDTO;
import com.contract_management.api.dto.request.SubstituicaoImpressoraRequestDTO;
import com.contract_management.api.dto.request.TrocaLocalRequestDTO;
import com.contract_management.api.dto.response.EmpenhoImpressaoDTO;
import com.contract_management.api.dto.response.ImpressoraResponseDTO;
import com.contract_management.api.dto.response.LoteImpressaoDTO;
import com.contract_management.api.service.EmpenhoImpressaoService;
import com.contract_management.api.service.ImpressoraService;
import com.contract_management.api.service.LoteImpressaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.contract_management.api.dto.request.EmpenhoRequestDTO;
import com.contract_management.api.dto.response.LeituraContadorResponseDTO;
import com.contract_management.api.service.LeituraContadorService;

import java.util.List;

@RestController
@RequestMapping("/impressoras")
@RequiredArgsConstructor
@Tag(name = "Impressoras", description = "Gestão do Parque de Impressoras e Locação")
public class ImpressoraController {

    private final ImpressoraService impressoraService;
    private final LoteImpressaoService loteService;
    private final EmpenhoImpressaoService empenhoService;
    private final LeituraContadorService leituraService;

    @GetMapping
    @Operation(summary = "Lista todas as impressoras ativas com suas instalações atuais")
    public ResponseEntity<List<ImpressoraResponseDTO>> listarTodas() {
        return ResponseEntity.ok(impressoraService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de uma impressora por ID")
    public ResponseEntity<ImpressoraResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(impressoraService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova impressora com sua alocação/instalação inicial")
    public ResponseEntity<ImpressoraResponseDTO> criar(@Valid @RequestBody ImpressoraRequestDTO dto) {
        ImpressoraResponseDTO criada = impressoraService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados cadastrais de uma impressora ou instalação")
    public ResponseEntity<ImpressoraResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ImpressoraRequestDTO dto) {
        return ResponseEntity.ok(impressoraService.atualizar(id, dto));
    }

    @PostMapping("/{id}/remanejar")
    @Operation(summary = "Registra a troca de setor/secretaria de uma impressora com histórico")
    public ResponseEntity<ImpressoraResponseDTO> remanejarLocal(@PathVariable Long id, @Valid @RequestBody TrocaLocalRequestDTO dto) {
        return ResponseEntity.ok(impressoraService.remanejarLocal(id, dto));
    }

    @PostMapping("/{id}/substituir")
    @Operation(summary = "Registra a substituição de máquina com defeito por nova máquina (Swap)")
    public ResponseEntity<ImpressoraResponseDTO> substituirPorDefeito(@PathVariable Long id, @Valid @RequestBody SubstituicaoImpressoraRequestDTO dto) {
        return ResponseEntity.ok(impressoraService.substituirPorDefeito(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativa / recolhe uma impressora do parque")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        impressoraService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/leituras")
    @Operation(summary = "Lista o histórico cronológico de leituras de contadores de uma impressora")
    public ResponseEntity<List<LeituraContadorResponseDTO>> listarLeiturasPorImpressora(@PathVariable Long id) {
        return ResponseEntity.ok(leituraService.listarPorImpressora(id));
    }

    @GetMapping("/lotes")
    @Operation(summary = "Lista os lotes contratuais de locação de impressoras")
    public ResponseEntity<List<LoteImpressaoDTO>> listarLotes() {
        return ResponseEntity.ok(loteService.listarTodos());
    }

    @GetMapping("/empenhos")
    @Operation(summary = "Lista os empenhos orçamentários de locação de impressoras")
    public ResponseEntity<List<EmpenhoImpressaoDTO>> listarEmpenhos(@RequestParam(required = false) Long secretariaId) {
        if (secretariaId != null) {
            return ResponseEntity.ok(empenhoService.listarPorSecretaria(secretariaId));
        }
        return ResponseEntity.ok(empenhoService.listarTodos());
    }

    @GetMapping("/empenhos/{id}")
    @Operation(summary = "Busca detalhes de um empenho por ID")
    public ResponseEntity<EmpenhoImpressaoDTO> buscarEmpenhoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empenhoService.buscarPorId(id));
    }

    @PostMapping("/empenhos")
    @Operation(summary = "Cadastra um novo empenho orçamentário")
    public ResponseEntity<EmpenhoImpressaoDTO> cadastrarEmpenho(@Valid @RequestBody EmpenhoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empenhoService.cadastrar(dto));
    }

    @PutMapping("/empenhos/{id}")
    @Operation(summary = "Atualiza dados de um empenho orçamentário")
    public ResponseEntity<EmpenhoImpressaoDTO> atualizarEmpenho(@PathVariable Long id, @Valid @RequestBody EmpenhoRequestDTO dto) {
        return ResponseEntity.ok(empenhoService.atualizar(id, dto));
    }

    @DeleteMapping("/empenhos/{id}")
    @Operation(summary = "Inativa um empenho orçamentário")
    public ResponseEntity<Void> excluirEmpenho(@PathVariable Long id) {
        empenhoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

