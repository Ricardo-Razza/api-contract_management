package com.contract_management.api.modules.impressora.service.financeiro;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.impressora.model.*;
import com.contract_management.api.modules.impressora.repository.EmpenhoImpressaoRepository;
import com.contract_management.api.modules.impressora.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.LeituraContadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceiroImpressaoTest {
    @Mock EmpenhoImpressaoRepository empenhos;
    @Mock InstalacaoImpressoraRepository instalacoes;
    @Mock LeituraContadorRepository leituras;
    FaturamentoImpressaoService faturamento;
    ExecucaoOrcamentariaService execucao;
    EmpenhoImpressao empenho;
    InstalacaoImpressora instalacao;
    LeituraContador leitura;

    @BeforeEach
    void preparar() {
        faturamento = new FaturamentoImpressaoService(empenhos, instalacoes, leituras);
        execucao = new ExecucaoOrcamentariaService(empenhos, instalacoes, leituras);
        empenho = EmpenhoImpressao.builder().id(1L).numeroEmpenho("2625").ano(2026)
                .valorTotal(new BigDecimal("1000.00")).build();
        var lote = LoteImpressao.builder().numeroLote(3)
                .valorLocacaoMensal(new BigDecimal("100.00"))
                .valorExcedenteMono(new BigDecimal("0.10"))
                .valorExcedenteColor(new BigDecimal("0.20")).build();
        var impressora = Impressora.builder().id(10L).itemPedido(1).modelo("Modelo colorido").lote(lote).build();
        instalacao = InstalacaoImpressora.builder().id(20L).impressora(impressora)
                .empenho(empenho).status("ATIVA").localInstalacao("Setor").build();
        leitura = LeituraContador.builder().impressora(impressora).instalacao(instalacao)
                .mesReferencia(8).anoReferencia(2026).proporcao(new BigDecimal("0.50"))
                .copiasMono(110).copiasColor(60).excedenteMono(10).excedenteColor(10)
                .valorLocacao(new BigDecimal("50.00"))
                .valorExcedenteMono(new BigDecimal("1.00"))
                .valorExcedenteColor(new BigDecimal("2.00"))
                .valorTotal(new BigDecimal("53.00")).build();
    }

    @Test
    void espelhoPreservaProporcaoExcedentesCodigosECompetenciaPadrao() {
        when(empenhos.findById(1L)).thenReturn(Optional.of(empenho));
        when(leituras.findByMesAndAnoAndEmpenhoIdWithDetails(8, 2026, 1L)).thenReturn(List.of(leitura));
        var fatura = faturamento.gerarEspelhoFatura(1L, null, null);
        assertEquals("Agosto / 2026", fatura.getCompetenciaFormatada());
        assertEquals(new BigDecimal("53.00"), fatura.getTotalFatura());
        assertEquals(List.of("41478", "41479", "41480"), fatura.getItens().stream().map(i -> i.getCodigoItem()).toList());
        assertEquals(new BigDecimal("0.50"), fatura.getItens().getFirst().getQuantidade());
        assertEquals(new BigDecimal("3.00"), fatura.getEquipamentos().getFirst().getValorExcedente());
        assertTrue(fatura.getTextoAtesto().contains("23/2024"));
    }

    @Test
    void espelhoPreservaBuscaDeLeiturasSemVinculoDeInstalacao() {
        leitura.setInstalacao(null);
        var outra = LeituraContador.builder().impressora(Impressora.builder().id(99L).build()).build();
        when(empenhos.findById(1L)).thenReturn(Optional.of(empenho));
        when(leituras.findByMesAndAnoAndEmpenhoIdWithDetails(8, 2026, 1L)).thenReturn(List.of());
        when(instalacoes.findByEmpenhoIdAndStatus(1L, "ATIVA")).thenReturn(List.of(instalacao));
        when(leituras.findByMesAndAnoWithDetails(8, 2026)).thenReturn(List.of(leitura, outra));
        var fatura = faturamento.gerarEspelhoFatura(1L, 8, 2026);
        assertEquals(new BigDecimal("53.00"), fatura.getTotalFatura());
        assertEquals(1, fatura.getEquipamentos().size());
        assertEquals("-", fatura.getEquipamentos().getFirst().getLocalInstalacao());
    }

    @Test
    void empenhoAusenteMantemErroDeNaoEncontrado() {
        when(empenhos.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> faturamento.gerarEspelhoFatura(99L, 8, 2026));
    }

    @Test
    void consolidadoPreservaDozeMesesETotaisDeLocacaoEExcedentes() {
        when(empenhos.findByAtivoTrueOrderByNumeroEmpenhoAsc()).thenReturn(new ArrayList<>(List.of(empenho)));
        when(leituras.findByAnoWithDetails(2026)).thenReturn(List.of(leitura));
        when(instalacoes.findByEmpenhoIdAndStatus(1L, "ATIVA")).thenReturn(List.of(instalacao));
        var consolidado = faturamento.obterNotasFiscaisConsolidado(null);
        assertEquals(2026, consolidado.getAno());
        assertEquals(12, consolidado.getTotaisPrefeituraMensais().size());
        assertEquals(new BigDecimal("53.00"), consolidado.getTotaisPrefeituraMensais().get(7));
        assertEquals(new BigDecimal("53.00"), consolidado.getTotalPrefeituraAnual());
        assertEquals(0, consolidado.getTotaisPrefeituraMensais().getFirst().signum());
    }

    @Test
    void execucaoPreservaValoresRealizadosProjecoesESaldo() {
        when(empenhos.findByAtivoTrueOrderByNumeroEmpenhoAsc()).thenReturn(List.of(empenho));
        when(leituras.findByAnoWithDetails(2026)).thenReturn(List.of(leitura));
        when(instalacoes.findByEmpenhoIdAndStatus(1L, "ATIVA")).thenReturn(List.of(instalacao));
        var resultado = execucao.obterExecucaoMensal(null);
        var meses = resultado.getEmpenhos().getFirst().getMeses();
        assertEquals("REALIZADO", meses.get(7).getStatus());
        assertEquals("PREVISTO", meses.get(9).getStatus());
        assertEquals("SEM_FATURAMENTO", meses.getFirst().getStatus());
        assertEquals(new BigDecimal("53.00"), resultado.getTotalGeralLiquidado());
        assertEquals(new BigDecimal("353.00"), resultado.getTotalGeralProjetado());
        assertEquals(new BigDecimal("947.00"), resultado.getSaldoGeralRestante());
    }

    @Test
    void execucaoPreservaExcecaoDeInstalacaoInicialDoEmpenhoSmed() {
        empenho.setNumeroEmpenho("2521");
        when(empenhos.findByAtivoTrueOrderByNumeroEmpenhoAsc()).thenReturn(List.of(empenho));
        when(leituras.findByAnoWithDetails(2026)).thenReturn(List.of());
        when(instalacoes.findByEmpenhoIdAndStatus(1L, "ATIVA")).thenReturn(List.of());
        var resultado = execucao.obterExecucaoMensal(2026);
        assertEquals(new BigDecimal("765.00"), resultado.getTotalGeralLiquidado());
        assertEquals("REALIZADO", resultado.getEmpenhos().getFirst().getMeses().get(1).getStatus());
    }
}
