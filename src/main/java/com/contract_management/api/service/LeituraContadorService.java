package com.contract_management.api.service;

import com.contract_management.api.dto.request.LeituraContadorRequestDTO;
import com.contract_management.api.dto.response.LeituraContadorResponseDTO;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.*;
import com.contract_management.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeituraContadorService {

    private final LeituraContadorRepository leituraRepository;
    private final ImpressoraRepository impressoraRepository;
    private final InstalacaoImpressoraRepository instalacaoRepository;

    @Transactional(readOnly = true)
    public List<LeituraContadorResponseDTO> listarPorMesEAno(Integer mes, Integer ano) {
        return leituraRepository.findByMesAndAnoWithDetails(mes, ano).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeituraContadorResponseDTO> listarPorImpressora(Long impressoraId) {
        return leituraRepository.findUltimasLeiturasPorImpressora(impressoraId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeituraContadorResponseDTO lancarLeitura(LeituraContadorRequestDTO dto) {
        Impressora impressora = impressoraRepository.findById(dto.getImpressoraId())
                .orElseThrow(() -> new EntityNotFoundException("Impressora", dto.getImpressoraId()));

        InstalacaoImpressora instalacao = instalacaoRepository.findAtivaByImpressoraId(impressora.getId())
                .orElse(null);

        // Busca leitura anterior para calcular cópias rodadas
        List<LeituraContador> anteriores = leituraRepository.findUltimasLeiturasPorImpressora(impressora.getId());
        int leituraMonoAnterior = 0;
        int leituraColorAnterior = 0;

        if (!anteriores.isEmpty()) {
            // Pega a mais recente diferente da atual se já existir
            LeituraContador maisRecente = anteriores.get(0);
            if (maisRecente.getMesReferencia().equals(dto.getMesReferencia()) && maisRecente.getAnoReferencia().equals(dto.getAnoReferencia())) {
                if (anteriores.size() > 1) {
                    leituraMonoAnterior = anteriores.get(1).getLeituraMonoAtual();
                    leituraColorAnterior = anteriores.get(1).getLeituraColorAtual();
                } else if (instalacao != null) {
                    leituraMonoAnterior = instalacao.getContadorInstalacaoMono();
                    leituraColorAnterior = instalacao.getContadorInstalacaoColor();
                }
            } else {
                leituraMonoAnterior = maisRecente.getLeituraMonoAtual();
                leituraColorAnterior = maisRecente.getLeituraColorAtual();
            }
        } else if (instalacao != null) {
            leituraMonoAnterior = instalacao.getContadorInstalacaoMono();
            leituraColorAnterior = instalacao.getContadorInstalacaoColor();
        }

        int copiasMono = Math.max(0, dto.getLeituraMonoAtual() - leituraMonoAnterior);
        int leituraColorAtual = dto.getLeituraColorAtual() != null ? dto.getLeituraColorAtual() : 0;
        int copiasColor = Math.max(0, leituraColorAtual - leituraColorAnterior);

        BigDecimal proporcao = dto.getProporcao() != null ? dto.getProporcao() : BigDecimal.ONE;

        // Se houver máquina(s) retirada(s) por Swap no mesmo mês e itemPedido, consolida as cópias
        int copiasMonoSwap = 0;
        int copiasColorSwap = 0;
        if (impressora.getItemPedido() != null) {
            List<LeituraContador> swaps = leituraRepository.findSwapsRetiradaPorItemPedidoEMes(
                    impressora.getItemPedido(), dto.getMesReferencia(), dto.getAnoReferencia(), impressora.getId());
            copiasMonoSwap = swaps.stream().mapToInt(LeituraContador::getCopiasMono).sum();
            copiasColorSwap = swaps.stream().mapToInt(LeituraContador::getCopiasColor).sum();
        }

        int copiasMonoCombinadas = copiasMono + copiasMonoSwap;
        int copiasColorCombinadas = copiasColor + copiasColorSwap;

        // Regras de Lote e Franquia
        int franquiaMono = 0;
        int franquiaColor = 0;
        int excedenteMono = 0;
        int excedenteColor = 0;
        BigDecimal valorLocacao = BigDecimal.ZERO;
        BigDecimal valorExcMono = BigDecimal.ZERO;
        BigDecimal valorExcColor = BigDecimal.ZERO;

        if (impressora.getLote() != null) {
            LoteImpressao lote = impressora.getLote();
            franquiaMono = BigDecimal.valueOf(lote.getFranquiaMono()).multiply(proporcao).setScale(0, RoundingMode.HALF_UP).intValue();
            franquiaColor = BigDecimal.valueOf(lote.getFranquiaColor()).multiply(proporcao).setScale(0, RoundingMode.HALF_UP).intValue();

            excedenteMono = Math.max(0, copiasMonoCombinadas - franquiaMono);
            excedenteColor = Math.max(0, copiasColorCombinadas - franquiaColor);

            valorLocacao = lote.getValorLocacaoMensal().multiply(proporcao).setScale(2, RoundingMode.HALF_UP);
            valorExcMono = lote.getValorExcedenteMono().multiply(BigDecimal.valueOf(excedenteMono)).setScale(2, RoundingMode.HALF_UP);
            valorExcColor = lote.getValorExcedenteColor().multiply(BigDecimal.valueOf(excedenteColor)).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal valorTotal = valorLocacao.add(valorExcMono).add(valorExcColor);

        // Atualiza leitura existente ou cria nova
        LeituraContador leitura = leituraRepository
                .findByImpressoraIdAndMesReferenciaAndAnoReferencia(impressora.getId(), dto.getMesReferencia(), dto.getAnoReferencia())
                .orElse(LeituraContador.builder()
                        .impressora(impressora)
                        .instalacao(instalacao)
                        .mesReferencia(dto.getMesReferencia())
                        .anoReferencia(dto.getAnoReferencia())
                        .build());

        leitura.setInstalacao(instalacao);
        leitura.setDataLeitura(dto.getDataLeitura());
        leitura.setLeituraMonoAnterior(leituraMonoAnterior);
        leitura.setLeituraMonoAtual(dto.getLeituraMonoAtual());
        leitura.setCopiasMono(copiasMono);
        leitura.setLeituraColorAnterior(leituraColorAnterior);
        leitura.setLeituraColorAtual(leituraColorAtual);
        leitura.setCopiasColor(copiasColor);
        leitura.setProporcao(proporcao);
        leitura.setFranquiaMonoAplicada(franquiaMono);
        leitura.setFranquiaColorAplicada(franquiaColor);
        leitura.setExcedenteMono(excedenteMono);
        leitura.setExcedenteColor(excedenteColor);
        leitura.setValorLocacao(valorLocacao);
        leitura.setValorExcedenteMono(valorExcMono);
        leitura.setValorExcedenteColor(valorExcColor);
        leitura.setValorTotal(valorTotal);
        leitura.setOrigemLeitura(dto.getOrigemLeitura() != null ? dto.getOrigemLeitura() : "MANUAL");

        if (copiasMonoSwap > 0 || copiasColorSwap > 0) {
            String obsSwap = String.format("Consolidação de Swap (Item %d): %d cópias mono nesta máquina + %d cópias da máquina substituída = %d cópias totais no período.",
                    impressora.getItemPedido(), copiasMono, copiasMonoSwap, copiasMonoCombinadas);
            if (dto.getObservacoes() != null && !dto.getObservacoes().isBlank()) {
                leitura.setObservacoes(dto.getObservacoes() + " | " + obsSwap);
            } else {
                leitura.setObservacoes(obsSwap);
            }
        } else {
            leitura.setObservacoes(dto.getObservacoes());
        }

        LeituraContador salva = leituraRepository.save(leitura);
        return toResponseDTO(salva);
    }

    private LeituraContadorResponseDTO toResponseDTO(LeituraContador l) {
        LeituraContadorResponseDTO.LeituraContadorResponseDTOBuilder builder = LeituraContadorResponseDTO.builder()
                .id(l.getId())
                .impressoraId(l.getImpressora().getId())
                .itemPedido(l.getImpressora().getItemPedido())
                .impressoraModelo(l.getImpressora().getModelo())
                .impressoraIp(l.getImpressora().getIp())
                .mesReferencia(l.getMesReferencia())
                .anoReferencia(l.getAnoReferencia())
                .dataLeitura(l.getDataLeitura())
                .leituraMonoAnterior(l.getLeituraMonoAnterior())
                .leituraMonoAtual(l.getLeituraMonoAtual())
                .copiasMono(l.getCopiasMono())
                .leituraColorAnterior(l.getLeituraColorAnterior())
                .leituraColorAtual(l.getLeituraColorAtual())
                .copiasColor(l.getCopiasColor())
                .proporcao(l.getProporcao())
                .franquiaMonoAplicada(l.getFranquiaMonoAplicada())
                .franquiaColorAplicada(l.getFranquiaColorAplicada())
                .excedenteMono(l.getExcedenteMono())
                .excedenteColor(l.getExcedenteColor())
                .valorLocacao(l.getValorLocacao())
                .valorExcedenteMono(l.getValorExcedenteMono())
                .valorExcedenteColor(l.getValorExcedenteColor())
                .valorTotal(l.getValorTotal())
                .origemLeitura(l.getOrigemLeitura())
                .observacoes(l.getObservacoes());

        if (l.getInstalacao() != null) {
            builder.localInstalacao(l.getInstalacao().getLocalInstalacao());
            if (l.getInstalacao().getSecretaria() != null) {
                builder.secretariaSigla(l.getInstalacao().getSecretaria().getSigla());
            }
        }

        return builder.build();
    }
}
