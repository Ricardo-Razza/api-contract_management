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
import com.contract_management.api.dto.response.*;
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

    @GetMapping("/empenhos/execucao-mensal")
    @Operation(summary = "Obtém a matriz de execução orçamentária mensal de todos os empenhos do ano")
    public ResponseEntity<ExecucaoMensalDTO> obterExecucaoMensal(@RequestParam(required = false, defaultValue = "2026") Integer ano) {
        return ResponseEntity.ok(empenhoService.obterExecucaoMensal(ano));
    }

    @GetMapping("/empenhos/{id}/espelho-fatura")
    @Operation(summary = "Gera o espelho da fatura e termo de atesto mensal por empenho")
    public ResponseEntity<EspelhoFaturaDTO> gerarEspelhoFatura(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "8") Integer mes,
            @RequestParam(required = false, defaultValue = "2026") Integer ano) {
        return ResponseEntity.ok(empenhoService.gerarEspelhoFatura(id, mes, ano));
    }

    @GetMapping("/notas-fiscais")
    @Operation(summary = "Obtém a matriz consolidada anual de notas fiscais por empenho e lote")
    public ResponseEntity<NotasFiscaisConsolidadoDTO> obterNotasFiscaisConsolidado(
            @RequestParam(required = false, defaultValue = "2026") Integer ano) {
        return ResponseEntity.ok(empenhoService.obterNotasFiscaisConsolidado(ano));
    }

    @GetMapping("/notas-fiscais/lote")
    @Operation(summary = "Gera o conjunto de espelhos de fatura e termos de atesto dos empenhos para um ou mais meses")
    public ResponseEntity<List<EspelhoFaturaDTO>> gerarNotasFiscaisLote(
            @RequestParam(required = false) List<Integer> meses,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false, defaultValue = "2026") Integer ano,
            @RequestParam(required = false) Long empenhoId) {
        return ResponseEntity.ok(empenhoService.gerarNotasFiscaisLote(meses, mes, ano, empenhoId));
    }

    @GetMapping("/lotes/balanco-franquias")
    @Operation(summary = "Gera o balanço mensal de franquias e custos por lote contratual")
    public ResponseEntity<BalancoFranquiasDTO> gerarBalancoFranquias(
            @RequestParam(required = false, defaultValue = "8") Integer mes,
            @RequestParam(required = false, defaultValue = "2026") Integer ano) {
        return ResponseEntity.ok(loteService.gerarBalancoFranquias(mes, ano));
    }

    @PostMapping("/sincronizar-planilha")
    @Operation(summary = "Sincroniza os dados expurgando duplicidades e garantindo paridade com a planilha oficial 2026")
    public ResponseEntity<java.util.Map<String, String>> sincronizarPlanilha() {
        empenhoService.expurgarItensDuplicadosPlanilha();
        return ResponseEntity.ok(java.util.Map.of(
                "status", "SUCESSO",
                "mensagem", "Duplicidades expurgadas e cálculos sincronizados com a planilha oficial"
        ));
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

