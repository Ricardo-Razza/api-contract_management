package com.contract_management.api.service;

import com.contract_management.api.dto.response.BalancoFranquiasDTO;
import com.contract_management.api.dto.response.LoteBalancoDTO;
import com.contract_management.api.dto.response.LoteImpressaoDTO;
import com.contract_management.api.model.LeituraContador;
import com.contract_management.api.model.LoteImpressao;
import com.contract_management.api.repository.ImpressoraRepository;
import com.contract_management.api.repository.LeituraContadorRepository;
import com.contract_management.api.repository.LoteImpressaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoteImpressaoService {

    private final LoteImpressaoRepository loteRepository;
    private final LeituraContadorRepository leituraRepository;
    private final ImpressoraRepository impressoraRepository;

    private static final String[] MESES_NOMES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    @Transactional(readOnly = true)
    public List<LoteImpressaoDTO> listarTodos() {
        return loteRepository.findByAtivoTrueOrderByNumeroLoteAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BalancoFranquiasDTO gerarBalancoFranquias(Integer mes, Integer ano) {
        if (mes == null) mes = 8;
        if (ano == null) ano = 2026;

        List<LoteImpressao> lotes = loteRepository.findByAtivoTrueOrderByNumeroLoteAsc();
        List<LeituraContador> leituras = leituraRepository.findByMesAndAnoWithDetails(mes, ano);

        Map<Long, List<LeituraContador>> leiturasPorLote = new HashMap<>();
        for (LeituraContador l : leituras) {
            if (l.getImpressora() != null && l.getImpressora().getLote() != null) {
                leiturasPorLote.computeIfAbsent(l.getImpressora().getLote().getId(), k -> new ArrayList<>()).add(l);
            }
        }

        List<LoteBalancoDTO> lotesBalanco = new ArrayList<>();
        int totalGeralEquipamentos = 0;
        int totalGeralCopiasMono = 0;
        int totalGeralCopiasColor = 0;
        int totalGeralExcedenteMono = 0;
        int totalGeralExcedenteColor = 0;
        BigDecimal custoTotalLocacao = BigDecimal.ZERO;
        BigDecimal custoTotalExcedentes = BigDecimal.ZERO;
        BigDecimal custoTotalGeral = BigDecimal.ZERO;

        for (LoteImpressao lote : lotes) {
            List<LeituraContador> leiturasLote = leiturasPorLote.getOrDefault(lote.getId(), Collections.emptyList());
            long countEquip = impressoraRepository.countByLoteIdAndAtivoTrue(lote.getId());
            if (countEquip == 0 && !leiturasLote.isEmpty()) {
                countEquip = leiturasLote.size();
            }

            int qtdEquip = (int) countEquip;
            int franquiaTotalMono = qtdEquip * lote.getFranquiaMono();
            int franquiaTotalColor = qtdEquip * lote.getFranquiaColor();

            int copiasMono = leiturasLote.stream().mapToInt(LeituraContador::getCopiasMono).sum();
            int copiasColor = leiturasLote.stream().mapToInt(LeituraContador::getCopiasColor).sum();
            int excMono = leiturasLote.stream().mapToInt(LeituraContador::getExcedenteMono).sum();
            int excColor = leiturasLote.stream().mapToInt(LeituraContador::getExcedenteColor).sum();

            BigDecimal custoLocacao = leiturasLote.stream()
                    .map(LeituraContador::getValorLocacao)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Se não houver leitura no mês para este lote mas houver equipamentos, calcula a locação teórica
            if (leiturasLote.isEmpty() && qtdEquip > 0) {
                custoLocacao = lote.getValorLocacaoMensal().multiply(BigDecimal.valueOf(qtdEquip));
            }

            BigDecimal custoExcMono = leiturasLote.stream()
                    .map(LeituraContador::getValorExcedenteMono)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal custoExcColor = leiturasLote.stream()
                    .map(LeituraContador::getValorExcedenteColor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal custoTotalLote = custoLocacao.add(custoExcMono).add(custoExcColor);

            Double percentualUsoMono = 0.0;
            if (franquiaTotalMono > 0) {
                percentualUsoMono = BigDecimal.valueOf(copiasMono)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(franquiaTotalMono), 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            Double percentualUsoColor = 0.0;
            if (franquiaTotalColor > 0) {
                percentualUsoColor = BigDecimal.valueOf(copiasColor)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(franquiaTotalColor), 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            totalGeralEquipamentos += qtdEquip;
            totalGeralCopiasMono += copiasMono;
            totalGeralCopiasColor += copiasColor;
            totalGeralExcedenteMono += excMono;
            totalGeralExcedenteColor += excColor;
            custoTotalLocacao = custoTotalLocacao.add(custoLocacao);
            custoTotalExcedentes = custoTotalExcedentes.add(custoExcMono).add(custoExcColor);
            custoTotalGeral = custoTotalGeral.add(custoTotalLote);

            lotesBalanco.add(LoteBalancoDTO.builder()
                    .loteId(lote.getId())
                    .numeroLote(lote.getNumeroLote())
                    .descricao(lote.getDescricao())
                    .tipo(lote.getTipo())
                    .quantidadeEquipamentos(qtdEquip)
                    .franquiaIndividualMono(lote.getFranquiaMono())
                    .franquiaIndividualColor(lote.getFranquiaColor())
                    .franquiaTotalMono(franquiaTotalMono)
                    .franquiaTotalColor(franquiaTotalColor)
                    .copiasMonoProduzidas(copiasMono)
                    .copiasColorProduzidas(copiasColor)
                    .excedenteMonoTotal(excMono)
                    .excedenteColorTotal(excColor)
                    .percentualUsoMono(percentualUsoMono)
                    .percentualUsoColor(percentualUsoColor)
                    .valorLocacaoUnitario(lote.getValorLocacaoMensal())
                    .valorExcedenteMonoUnitario(lote.getValorExcedenteMono())
                    .valorExcedenteColorUnitario(lote.getValorExcedenteColor())
                    .custoFixoLocacao(custoLocacao)
                    .custoExcedenteMono(custoExcMono)
                    .custoExcedenteColor(custoExcColor)
                    .custoTotal(custoTotalLote)
                    .build());
        }

        String competenciaFormatada = MESES_NOMES[mes - 1] + " / " + ano;

        return BalancoFranquiasDTO.builder()
                .mesReferencia(mes)
                .anoReferencia(ano)
                .competenciaFormatada(competenciaFormatada)
                .totalGeralEquipamentos(totalGeralEquipamentos)
                .totalGeralCopiasMono(totalGeralCopiasMono)
                .totalGeralCopiasColor(totalGeralCopiasColor)
                .totalGeralExcedenteMono(totalGeralExcedenteMono)
                .totalGeralExcedenteColor(totalGeralExcedenteColor)
                .custoTotalLocacao(custoTotalLocacao)
                .custoTotalExcedentes(custoTotalExcedentes)
                .custoTotalGeral(custoTotalGeral)
                .lotes(lotesBalanco)
                .build();
    }

    private LoteImpressaoDTO toDTO(LoteImpressao l) {
        return LoteImpressaoDTO.builder()
                .id(l.getId())
                .numeroLote(l.getNumeroLote())
                .descricao(l.getDescricao())
                .tipo(l.getTipo())
                .franquiaMono(l.getFranquiaMono())
                .franquiaColor(l.getFranquiaColor())
                .valorLocacaoMensal(l.getValorLocacaoMensal())
                .valorExcedenteMono(l.getValorExcedenteMono())
                .valorExcedenteColor(l.getValorExcedenteColor())
                .ativo(l.getAtivo())
                .build();
    }
}
