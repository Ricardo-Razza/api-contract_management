package com.contract_management.api.modules.impressora.service.medicao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.impressora.dto.request.LeituraContadorRequestDTO;
import com.contract_management.api.modules.impressora.dto.response.ItemGradeLeituraDTO;
import com.contract_management.api.modules.impressora.dto.response.LeituraContadorResponseDTO;
import com.contract_management.api.modules.impressora.model.Impressora;
import com.contract_management.api.modules.impressora.model.InstalacaoImpressora;
import com.contract_management.api.modules.impressora.model.LeituraContador;
import com.contract_management.api.modules.impressora.model.LoteImpressao;
import com.contract_management.api.modules.impressora.repository.ImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.LeituraContadorRepository;

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

    @Transactional(readOnly = true)
    public List<ItemGradeLeituraDTO> obterGradeLeituras(Integer mes, Integer ano) {
        List<InstalacaoImpressora> instalacoes = instalacaoRepository.findAllAtivasWithDetails();
        List<LeituraContador> leiturasMes = leituraRepository.findByMesAndAnoWithDetails(mes, ano);
        List<LeituraContador> todasAnteriores = leituraRepository.findAllLeiturasAnteriores(mes, ano);

        Map<Long, LeituraContador> leituraAtualMap = leiturasMes.stream()
                .filter(l -> l.getImpressora() != null)
                .collect(Collectors.toMap(l -> l.getImpressora().getId(), l -> l, (a, b) -> a));

        Map<Long, List<LeituraContador>> anterioresMap = todasAnteriores.stream()
                .filter(l -> l.getImpressora() != null)
                .collect(Collectors.groupingBy(l -> l.getImpressora().getId()));

        LocalDate hoje = LocalDate.now();

        return instalacoes.stream()
                .map(inst -> {
                    Impressora imp = inst.getImpressora();
                    LoteImpressao lote = imp.getLote();
                    LeituraContador atual = leituraAtualMap.get(imp.getId());

                    int franquiaMono = lote != null ? lote.getFranquiaMono() : 0;
                    int franquiaColor = lote != null ? lote.getFranquiaColor() : 0;
                    BigDecimal valorLocacao = lote != null ? lote.getValorLocacaoMensal() : BigDecimal.ZERO;

                    ItemGradeLeituraDTO.ItemGradeLeituraDTOBuilder builder = ItemGradeLeituraDTO.builder()
                            .impressoraId(imp.getId())
                            .itemPedido(imp.getItemPedido())
                            .fabricante(imp.getFabricante())
                            .modelo(imp.getModelo())
                            .tipoImpressao(imp.getTipoImpressao() != null ? imp.getTipoImpressao() : "MONO")
                            .ip(imp.getIp())
                            .secretariaId(inst.getSecretaria() != null ? inst.getSecretaria().getId() : null)
                            .secretariaSigla(inst.getSecretaria() != null ? inst.getSecretaria().getSigla() : "")
                            .secretariaNome(inst.getSecretaria() != null ? inst.getSecretaria().getNome() : "")
                            .localInstalacao(inst.getLocalInstalacao())
                            .numeroLote(lote != null ? lote.getNumeroLote() : null)
                            .franquiaMono(franquiaMono)
                            .franquiaColor(franquiaColor);

                    if (atual != null) {
                        builder.leituraId(atual.getId())
                                .dataLeitura(atual.getDataLeitura())
                                .leituraMonoAnterior(atual.getLeituraMonoAnterior())
                                .leituraMonoAtual(atual.getLeituraMonoAtual())
                                .copiasMono(atual.getCopiasMono())
                                .leituraColorAnterior(atual.getLeituraColorAnterior())
                                .leituraColorAtual(atual.getLeituraColorAtual())
                                .copiasColor(atual.getCopiasColor())
                                .excedenteMono(atual.getExcedenteMono())
                                .excedenteColor(atual.getExcedenteColor())
                                .valorLocacao(atual.getValorLocacao())
                                .valorTotal(atual.getValorTotal())
                                .origemLeitura(atual.getOrigemLeitura())
                                .status("SALVO")
                                .observacoes(atual.getObservacoes());
                    } else {
                        List<LeituraContador> anteriores = anterioresMap.get(imp.getId());
                        int antMono = 0;
                        int antColor = 0;
                        if (anteriores != null && !anteriores.isEmpty()) {
                            antMono = anteriores.get(0).getLeituraMonoAtual();
                            antColor = anteriores.get(0).getLeituraColorAtual();
                        } else {
                            antMono = inst.getContadorInstalacaoMono() != null ? inst.getContadorInstalacaoMono() : 0;
                            antColor = inst.getContadorInstalacaoColor() != null ? inst.getContadorInstalacaoColor() : 0;
                        }

                        builder.leituraId(null)
                                .dataLeitura(hoje)
                                .leituraMonoAnterior(antMono)
                                .leituraMonoAtual(null)
                                .copiasMono(0)
                                .leituraColorAnterior(antColor)
                                .leituraColorAtual(null)
                                .copiasColor(0)
                                .excedenteMono(0)
                                .excedenteColor(0)
                                .valorLocacao(valorLocacao)
                                .valorTotal(valorLocacao)
                                .origemLeitura("MANUAL")
                                .status("PENDENTE")
                                .observacoes(null);
                    }

                    return builder.build();
                })
                .sorted(Comparator.comparing(ItemGradeLeituraDTO::getItemPedido, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ItemGradeLeituraDTO::getImpressoraId))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<LeituraContadorResponseDTO> salvarEmLote(List<LeituraContadorRequestDTO> dtos) {
        return dtos.stream()
                .map(this::lancarLeitura)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeituraContadorResponseDTO lancarLeitura(LeituraContadorRequestDTO dto) {
        Impressora impressora = impressoraRepository.findById(dto.getImpressoraId())
                .orElseThrow(() -> new EntityNotFoundException("Impressora", dto.getImpressoraId()));

        InstalacaoImpressora instalacao = instalacaoRepository.findAtivaByImpressoraId(impressora.getId())
                .orElse(null);

        // Busca leitura anterior para calcular cópias rodadas
        List<LeituraContador> anteriores = leituraRepository.findLeiturasAnteriores(impressora.getId(), dto.getMesReferencia(), dto.getAnoReferencia());
        int leituraMonoAnterior = 0;
        int leituraColorAnterior = 0;

        if (!anteriores.isEmpty()) {
            leituraMonoAnterior = anteriores.get(0).getLeituraMonoAtual();
            leituraColorAnterior = anteriores.get(0).getLeituraColorAtual();
        } else if (instalacao != null) {
            leituraMonoAnterior = instalacao.getContadorInstalacaoMono() != null ? instalacao.getContadorInstalacaoMono() : 0;
            leituraColorAnterior = instalacao.getContadorInstalacaoColor() != null ? instalacao.getContadorInstalacaoColor() : 0;
        }

        int monoAtual = dto.getLeituraMonoAtual() != null ? dto.getLeituraMonoAtual() : leituraMonoAnterior;
        int copiasMono = Math.max(0, monoAtual - leituraMonoAnterior);

        int colorAtual = dto.getLeituraColorAtual() != null ? dto.getLeituraColorAtual() : leituraColorAnterior;
        int copiasColor = Math.max(0, colorAtual - leituraColorAnterior);

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
        leitura.setLeituraMonoAtual(monoAtual);
        leitura.setCopiasMono(copiasMono);
        leitura.setLeituraColorAnterior(leituraColorAnterior);
        leitura.setLeituraColorAtual(colorAtual);
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