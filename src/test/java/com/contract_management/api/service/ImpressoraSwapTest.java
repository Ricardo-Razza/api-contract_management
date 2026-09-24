package com.contract_management.api.service;

import com.contract_management.api.dto.request.LeituraContadorRequestDTO;
import com.contract_management.api.dto.request.SubstituicaoImpressoraRequestDTO;
import com.contract_management.api.dto.response.ImpressoraResponseDTO;
import com.contract_management.api.dto.response.LeituraContadorResponseDTO;
import com.contract_management.api.model.*;
import com.contract_management.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImpressoraSwapTest {

    @Mock
    private ImpressoraRepository impressoraRepository;

    @Mock
    private InstalacaoImpressoraRepository instalacaoRepository;

    @Mock
    private LeituraContadorRepository leituraRepository;

    @InjectMocks
    private ImpressoraService impressoraService;

    @InjectMocks
    private LeituraContadorService leituraContadorService;

    private Impressora maquinaA;
    private Impressora maquinaB;
    private InstalacaoImpressora instalacaoA;
    private LoteImpressao lote1;

    @BeforeEach
    void setUp() {
        lote1 = LoteImpressao.builder()
                .id(1L)
                .numeroLote(1)
                .descricao("Lote 1 Mono")
                .franquiaMono(1000)
                .franquiaColor(0)
                .valorLocacaoMensal(new BigDecimal("30.00"))
                .valorExcedenteMono(new BigDecimal("0.0300"))
                .valorExcedenteColor(BigDecimal.ZERO)
                .build();

        maquinaA = Impressora.builder()
                .id(10L)
                .itemPedido(25)
                .numeroSerie("SERIE-AAA")
                .fabricante("Samsung")
                .modelo("M4070")
                .tipoImpressao("MONO")
                .lote(lote1)
                .ip("192.168.1.100")
                .ativo(true)
                .build();

        instalacaoA = InstalacaoImpressora.builder()
                .id(100L)
                .impressora(maquinaA)
                .localInstalacao("Recepção")
                .contadorInstalacaoMono(5000)
                .dataInstalacao(LocalDate.of(2026, 8, 1))
                .status("ATIVA")
                .build();

        maquinaB = Impressora.builder()
                .id(20L)
                .itemPedido(25)
                .numeroSerie("SERIE-BBB")
                .fabricante("Ricoh")
                .modelo("SP 3710")
                .tipoImpressao("MONO")
                .lote(lote1)
                .ip("192.168.1.100")
                .ativo(true)
                .build();
    }

    @Test
    void testSubstituirPorDefeitoGeraLeituraSwapRetiradaComValorZero() {
        when(impressoraRepository.findById(10L)).thenReturn(Optional.of(maquinaA));
        when(instalacaoRepository.findAtivaByImpressoraId(10L)).thenReturn(Optional.of(instalacaoA));
        when(leituraRepository.findUltimasLeiturasPorImpressora(10L)).thenReturn(Collections.emptyList());
        when(leituraRepository.findByImpressoraIdAndMesReferenciaAndAnoReferencia(10L, 8, 2026)).thenReturn(Optional.empty());

        when(impressoraRepository.save(any(Impressora.class))).thenAnswer(inv -> {
            Impressora imp = inv.getArgument(0);
            if (imp.getId() == null) imp.setId(20L);
            return imp;
        });
        when(instalacaoRepository.save(any(InstalacaoImpressora.class))).thenAnswer(inv -> inv.getArgument(0));

        SubstituicaoImpressoraRequestDTO dto = SubstituicaoImpressoraRequestDTO.builder()
                .contadorFinalMonoRetirada(5400) // rodou 400 cópias (5400 - 5000)
                .dataSubstituicao(LocalDate.of(2026, 8, 13))
                .motivoDefeito("Fusor queimado")
                .novoModelo("SP 3710")
                .novoNumeroSerie("SERIE-BBB")
                .contadorInicialMonoNova(10000)
                .build();

        ImpressoraResponseDTO resp = impressoraService.substituirPorDefeito(10L, dto);

        assertNotNull(resp);
        assertFalse(maquinaA.getAtivo());
        assertEquals("SUBSTITUIDA", instalacaoA.getStatus());
        assertEquals(5400, instalacaoA.getContadorRetiradaMono());

        ArgumentCaptor<LeituraContador> captor = ArgumentCaptor.forClass(LeituraContador.class);
        verify(leituraRepository).save(captor.capture());
        LeituraContador gravada = captor.getValue();

        assertEquals("SWAP_RETIRADA", gravada.getOrigemLeitura());
        assertEquals(400, gravada.getCopiasMono());
        assertEquals(5000, gravada.getLeituraMonoAnterior());
        assertEquals(5400, gravada.getLeituraMonoAtual());
        assertEquals(BigDecimal.ZERO, gravada.getProporcao());
        assertEquals(BigDecimal.ZERO, gravada.getValorLocacao());
        assertEquals(BigDecimal.ZERO, gravada.getValorTotal());
        assertEquals(0, gravada.getExcedenteMono());
        assertEquals(0, gravada.getFranquiaMonoAplicada());
    }

    @Test
    void testLancarLeituraConsolidaCopiasSwapEAplicaFranquiaUnica() {
        // Simula que a Máquina A tem registro de SWAP_RETIRADA com 400 cópias no mês 8/2026
        LeituraContador swapA = LeituraContador.builder()
                .id(501L)
                .impressora(maquinaA)
                .mesReferencia(8)
                .anoReferencia(2026)
                .copiasMono(400)
                .copiasColor(0)
                .origemLeitura("SWAP_RETIRADA")
                .build();

        InstalacaoImpressora instalacaoB = InstalacaoImpressora.builder()
                .id(200L)
                .impressora(maquinaB)
                .contadorInstalacaoMono(10000)
                .dataInstalacao(LocalDate.of(2026, 8, 13))
                .status("ATIVA")
                .build();

        when(impressoraRepository.findById(20L)).thenReturn(Optional.of(maquinaB));
        when(instalacaoRepository.findAtivaByImpressoraId(20L)).thenReturn(Optional.of(instalacaoB));
        when(leituraRepository.findUltimasLeiturasPorImpressora(20L)).thenReturn(Collections.emptyList());
        when(leituraRepository.findByImpressoraIdAndMesReferenciaAndAnoReferencia(20L, 8, 2026)).thenReturn(Optional.empty());

        // Retorna a leitura swap da máquina A quando buscar pelo itemPedido 25
        when(leituraRepository.findSwapsRetiradaPorItemPedidoEMes(25, 8, 2026, 20L))
                .thenReturn(List.of(swapA));

        when(leituraRepository.save(any(LeituraContador.class))).thenAnswer(inv -> inv.getArgument(0));

        // Leitura no fim do mês: máquina B rodou 800 cópias (10800 - 10000)
        LeituraContadorRequestDTO dto = LeituraContadorRequestDTO.builder()
                .impressoraId(20L)
                .mesReferencia(8)
                .anoReferencia(2026)
                .dataLeitura(LocalDate.of(2026, 8, 31))
                .leituraMonoAtual(10800)
                .proporcao(BigDecimal.ONE)
                .origemLeitura("MANUAL")
                .build();

        LeituraContadorResponseDTO resp = leituraContadorService.lancarLeitura(dto);

        assertNotNull(resp);
        // Cópias individuais da máquina B = 800
        assertEquals(800, resp.getCopiasMono());
        // Franquia do lote = 1000
        assertEquals(1000, resp.getFranquiaMonoAplicada());
        // Total combinado = 800 + 400 = 1200. Excedente = 1200 - 1000 = 200 cópias!
        assertEquals(200, resp.getExcedenteMono());
        // Locação = R$ 30,00
        assertEquals(new BigDecimal("30.00"), resp.getValorLocacao());
        // Excedente = 200 * 0.03 = R$ 6,00
        assertEquals(new BigDecimal("6.00"), resp.getValorExcedenteMono());
        // Valor total = 30.00 + 6.00 = R$ 36,00
        assertEquals(new BigDecimal("36.00"), resp.getValorTotal());
        // Observações detalham a consolidação
        assertTrue(resp.getObservacoes().contains("Consolidação de Swap"));
    }
}
