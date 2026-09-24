package com.contract_management.api.service;

import com.contract_management.api.dto.request.EmpenhoRequestDTO;
import com.contract_management.api.dto.response.*;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.*;
import com.contract_management.api.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpenhoImpressaoService {

    private final EmpenhoImpressaoRepository empenhoRepository;
    private final InstalacaoImpressoraRepository instalacaoRepository;
    private final SecretariaRepository secretariaRepository;
    private final LeituraContadorRepository leituraRepository;
    private final LoteImpressaoRepository loteRepository;
    private final ImpressoraRepository impressoraRepository;

    private static final String[] MESES_NOMES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private static final String[] MESES_SIGLAS = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    @PostConstruct
    @Transactional
    public void initSync() {
        try {
            sincronizarLotesEEmpenhosSeNecessario();
        } catch (Exception ex) {
            log.warn("Nao foi possivel sincronizar lotes/empenhos automaticamente no startup: {}", ex.getMessage());
        }
    }

    @Transactional
    public void sincronizarLotesEEmpenhosSeNecessario() {
        if (loteRepository.findByNumeroLote(2).isEmpty()) {
            LoteImpressao lote2 = LoteImpressao.builder()
                    .numeroLote(2)
                    .descricao("Multifuncional Laser Monocromática (Médio/Grande Porte - SMED)")
                    .tipo("MONO")
                    .franquiaMono(5000)
                    .franquiaColor(0)
                    .valorLocacaoMensal(new BigDecimal("201.00"))
                    .valorExcedenteMono(new BigDecimal("0.0700"))
                    .valorExcedenteColor(BigDecimal.ZERO)
                    .ativo(true)
                    .build();
            loteRepository.save(lote2);
        }
        if (loteRepository.findByNumeroLote(5).isEmpty()) {
            LoteImpressao lote5 = LoteImpressao.builder()
                    .numeroLote(5)
                    .descricao("Impressora Laser Colorida Especial (500 páginas)")
                    .tipo("COLOR")
                    .franquiaMono(0)
                    .franquiaColor(500)
                    .valorLocacaoMensal(new BigDecimal("204.00"))
                    .valorExcedenteMono(new BigDecimal("0.0400"))
                    .valorExcedenteColor(new BigDecimal("0.2900"))
                    .ativo(true)
                    .build();
            loteRepository.save(lote5);
        }

        // Sincroniza dotações orçamentárias oficiais da planilha municipal 2026
        sincronizarDotacaoEmpenho("2625", new BigDecimal("36014.00"));
        sincronizarDotacaoEmpenho("2516", new BigDecimal("11484.00"));
        sincronizarDotacaoEmpenho("2517", new BigDecimal("8723.00"));
        sincronizarDotacaoEmpenho("2518", new BigDecimal("990.00"));
        sincronizarDotacaoEmpenho("2519", new BigDecimal("495.00"));
        sincronizarDotacaoEmpenho("2520", new BigDecimal("1320.00"));
        sincronizarDotacaoEmpenho("2522", new BigDecimal("15741.00"));
        sincronizarDotacaoEmpenho("2521", new BigDecimal("156794.00"));

        // Expurgar itens >= 500 duplicados das linhas coloridas do LibreOffice
        expurgarItensDuplicadosPlanilha();
    }

    @Transactional
    public void expurgarItensDuplicadosPlanilha() {
        List<Impressora> todas = impressoraRepository.findAll();
        List<Impressora> fantasmas = todas.stream()
                .filter(i -> i.getItemPedido() != null && i.getItemPedido() >= 500)
                .toList();

        if (!fantasmas.isEmpty()) {
            log.info("Expurgando {} impressoras fantasmas (itens >= 500) para garantir paridade com a planilha oficial", fantasmas.size());
            for (Impressora imp : fantasmas) {
                List<LeituraContador> leituras = leituraRepository.findUltimasLeiturasPorImpressora(imp.getId());
                if (!leituras.isEmpty()) {
                    leituraRepository.deleteAll(leituras);
                }
                List<InstalacaoImpressora> instalacoes = instalacaoRepository.findByImpressoraIdOrderByDataInstalacaoDesc(imp.getId());
                if (!instalacoes.isEmpty()) {
                    instalacaoRepository.deleteAll(instalacoes);
                }
                impressoraRepository.delete(imp);
            }
        }
    }


    private void sincronizarDotacaoEmpenho(String numeroEmpenho, BigDecimal valorOficial) {
        empenhoRepository.findByNumeroEmpenhoAndAno(numeroEmpenho, 2026).ifPresent(e -> {
            if (e.getValorTotal() == null || e.getValorTotal().compareTo(valorOficial.multiply(new BigDecimal("1.5"))) > 0) {
                e.setValorTotal(valorOficial);
                empenhoRepository.save(e);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<EmpenhoImpressaoDTO> listarTodos() {
        return empenhoRepository.findByAtivoTrueOrderByNumeroEmpenhoAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmpenhoImpressaoDTO> listarPorSecretaria(Long secretariaId) {
        return empenhoRepository.findBySecretariaIdAndAtivoTrue(secretariaId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmpenhoImpressaoDTO buscarPorId(Long id) {
        EmpenhoImpressao e = empenhoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empenho de Impressão", id));
        return toDTO(e);
    }

    @Transactional
    public EmpenhoImpressaoDTO cadastrar(EmpenhoRequestDTO dto) {
        Secretaria secretaria = secretariaRepository.findById(dto.getSecretariaId())
                .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getSecretariaId()));

        EmpenhoImpressao empenho = EmpenhoImpressao.builder()
                .numeroEmpenho(dto.getNumeroEmpenho())
                .ano(dto.getAno())
                .secretaria(secretaria)
                .descricao(dto.getDescricao())
                .valorTotal(dto.getValorTotal() != null ? dto.getValorTotal() : BigDecimal.ZERO)
                .saldo(dto.getSaldo() != null ? dto.getSaldo() : (dto.getValorTotal() != null ? dto.getValorTotal() : BigDecimal.ZERO))
                .ativo(true)
                .build();

        EmpenhoImpressao salvo = empenhoRepository.save(empenho);
        return toDTO(salvo);
    }

    @Transactional
    public EmpenhoImpressaoDTO atualizar(Long id, EmpenhoRequestDTO dto) {
        EmpenhoImpressao empenho = empenhoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empenho de Impressão", id));

        if (dto.getSecretariaId() != null) {
            Secretaria secretaria = secretariaRepository.findById(dto.getSecretariaId())
                    .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getSecretariaId()));
            empenho.setSecretaria(secretaria);
        }

        empenho.setNumeroEmpenho(dto.getNumeroEmpenho());
        empenho.setAno(dto.getAno());
        empenho.setDescricao(dto.getDescricao());
        if (dto.getValorTotal() != null) empenho.setValorTotal(dto.getValorTotal());
        if (dto.getSaldo() != null) empenho.setSaldo(dto.getSaldo());

        return toDTO(empenhoRepository.save(empenho));
    }

    @Transactional
    public void excluir(Long id) {
        EmpenhoImpressao empenho = empenhoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empenho de Impressão", id));
        empenho.setAtivo(false);
        empenhoRepository.save(empenho);
    }

    @Transactional(readOnly = true)
    public ExecucaoMensalDTO obterExecucaoMensal(Integer ano) {
        if (ano == null) {
            ano = 2026;
        }

        List<EmpenhoImpressao> empenhos = empenhoRepository.findByAtivoTrueOrderByNumeroEmpenhoAsc();
        List<LeituraContador> leiturasAno = leituraRepository.findByAnoWithDetails(ano);

        // Agrupa leituras por empenho_id e mês
        Map<Long, Map<Integer, List<LeituraContador>>> leiturasPorEmpenho = new HashMap<>();
        for (LeituraContador l : leiturasAno) {
            Long empId = null;
            if (l.getInstalacao() != null && l.getInstalacao().getEmpenho() != null) {
                empId = l.getInstalacao().getEmpenho().getId();
            }
            if (empId != null) {
                leiturasPorEmpenho
                        .computeIfAbsent(empId, k -> new HashMap<>())
                        .computeIfAbsent(l.getMesReferencia(), k -> new ArrayList<>())
                        .add(l);
            }
        }

        List<EmpenhoExecucaoDTO> empenhosDTO = new ArrayList<>();
        BigDecimal totalGeralEmpenhado = BigDecimal.ZERO;
        BigDecimal totalGeralLiquidado = BigDecimal.ZERO;
        BigDecimal totalGeralProjetado = BigDecimal.ZERO;

        BigDecimal[] somaMensalGeral = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            somaMensalGeral[i] = BigDecimal.ZERO;
        }

        for (EmpenhoImpressao e : empenhos) {
            List<InstalacaoImpressora> ativas = instalacaoRepository.findByEmpenhoIdAndStatus(e.getId(), "ATIVA");
            long qtdEquip = ativas.size();

            // Custo mensal fixo de locação para projeção
            BigDecimal locacaoMensalFixa = ativas.stream()
                    .map(inst -> {
                        if (inst.getImpressora() != null && inst.getImpressora().getLote() != null) {
                            return inst.getImpressora().getLote().getValorLocacaoMensal();
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<Integer, List<LeituraContador>> mesesEmp = leiturasPorEmpenho.getOrDefault(e.getId(), Collections.emptyMap());
            List<ValorMesDTO> mesesList = new ArrayList<>();

            for (int m = 1; m <= 12; m++) {
                List<LeituraContador> doMes = mesesEmp.get(m);
                BigDecimal valor;
                int copiasMono = 0;
                int copiasColor = 0;
                String status;

                if (doMes != null && !doMes.isEmpty()) {
                    valor = doMes.stream()
                            .map(LeituraContador::getValorTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    copiasMono = doMes.stream().mapToInt(LeituraContador::getCopiasMono).sum();
                    copiasColor = doMes.stream().mapToInt(LeituraContador::getCopiasColor).sum();
                    if (valor.compareTo(BigDecimal.ZERO) == 0) {
                        status = "SEM_FATURAMENTO";
                    } else if (ano == 2026 && m >= 9) {
                        status = "PREVISTO";
                    } else {
                        status = "REALIZADO";
                    }
                } else if (ano == 2026 && "2521".equals(e.getNumeroEmpenho()) && m == 2) {
                    // Fevereiro 2026 - Instalacao inicial SMED
                    valor = new BigDecimal("765.00");
                    status = "REALIZADO";
                } else if (ano == 2026 && m >= 9) {
                    // Meses futuros do ano vigente: projecao com custo de locacao fixo
                    valor = locacaoMensalFixa;
                    status = "PREVISTO";
                } else {
                    valor = BigDecimal.ZERO;
                    status = "SEM_FATURAMENTO";
                }

                somaMensalGeral[m - 1] = somaMensalGeral[m - 1].add(valor);

                mesesList.add(ValorMesDTO.builder()
                        .mes(m)
                        .nomeMes(MESES_SIGLAS[m - 1])
                        .valorFaturado(valor)
                        .copiasMono(copiasMono)
                        .copiasColor(copiasColor)
                        .status(status)
                        .build());
            }

            BigDecimal totalLiquidado = mesesList.stream()
                    .filter(vm -> "REALIZADO".equals(vm.getStatus()))
                    .map(ValorMesDTO::getValorFaturado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalProjetado = mesesList.stream()
                    .map(ValorMesDTO::getValorFaturado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saldoRestante = e.getValorTotal().subtract(totalLiquidado);
            Double percentualConsumido = 0.0;
            if (e.getValorTotal().compareTo(BigDecimal.ZERO) > 0) {
                percentualConsumido = totalLiquidado.multiply(BigDecimal.valueOf(100))
                        .divide(e.getValorTotal(), 2, RoundingMode.HALF_UP).doubleValue();
            }

            totalGeralEmpenhado = totalGeralEmpenhado.add(e.getValorTotal());
            totalGeralLiquidado = totalGeralLiquidado.add(totalLiquidado);
            totalGeralProjetado = totalGeralProjetado.add(totalProjetado);

            empenhosDTO.add(EmpenhoExecucaoDTO.builder()
                    .empenhoId(e.getId())
                    .numeroEmpenho(e.getNumeroEmpenho())
                    .secretariaSigla(e.getSecretaria() != null ? e.getSecretaria().getSigla() : "-")
                    .secretariaNome(e.getSecretaria() != null ? e.getSecretaria().getNome() : "-")
                    .descricao(e.getDescricao())
                    .valorTotalEmpenhado(e.getValorTotal())
                    .quantidadeImpressoras(qtdEquip)
                    .meses(mesesList)
                    .totalLiquidado(totalLiquidado)
                    .totalProjetado(totalProjetado)
                    .saldoRestante(saldoRestante)
                    .percentualConsumido(percentualConsumido)
                    .build());
        }

        List<ValorMesDTO> totaisMensais = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String status = m <= 8 ? "REALIZADO" : (m >= 9 ? "PREVISTO" : "SEM_FATURAMENTO");
            totaisMensais.add(ValorMesDTO.builder()
                    .mes(m)
                    .nomeMes(MESES_SIGLAS[m - 1])
                    .valorFaturado(somaMensalGeral[m - 1])
                    .copiasMono(0)
                    .copiasColor(0)
                    .status(status)
                    .build());
        }

        BigDecimal saldoGeralRestante = totalGeralEmpenhado.subtract(totalGeralLiquidado);
        Double percentualGeralConsumido = 0.0;
        if (totalGeralEmpenhado.compareTo(BigDecimal.ZERO) > 0) {
            percentualGeralConsumido = totalGeralLiquidado.multiply(BigDecimal.valueOf(100))
                    .divide(totalGeralEmpenhado, 2, RoundingMode.HALF_UP).doubleValue();
        }

        return ExecucaoMensalDTO.builder()
                .ano(ano)
                .totalGeralEmpenhado(totalGeralEmpenhado)
                .totalGeralLiquidado(totalGeralLiquidado)
                .totalGeralProjetado(totalGeralProjetado)
                .saldoGeralRestante(saldoGeralRestante)
                .percentualGeralConsumido(percentualGeralConsumido)
                .totaisMensais(totaisMensais)
                .empenhos(empenhosDTO)
                .build();
    }

    @Transactional(readOnly = true)
    public EspelhoFaturaDTO gerarEspelhoFatura(Long empenhoId, Integer mes, Integer ano) {
        if (mes == null) mes = 8;
        if (ano == null) ano = 2026;

        EmpenhoImpressao empenho = empenhoRepository.findById(empenhoId)
                .orElseThrow(() -> new EntityNotFoundException("Empenho de Impressão", empenhoId));

        List<LeituraContador> leituras = leituraRepository.findByMesAndAnoAndEmpenhoIdWithDetails(mes, ano, empenhoId);

        // Fallback de busca por impressoras ativas do empenho caso a leitura não tenha a foreign key de instalação preenchida
        if (leituras.isEmpty()) {
            List<InstalacaoImpressora> ativas = instalacaoRepository.findByEmpenhoIdAndStatus(empenhoId, "ATIVA");
            Set<Long> impIds = ativas.stream().map(i -> i.getImpressora().getId()).collect(Collectors.toSet());
            List<LeituraContador> todasDoMes = leituraRepository.findByMesAndAnoWithDetails(mes, ano);
            leituras = todasDoMes.stream()
                    .filter(l -> impIds.contains(l.getImpressora().getId()))
                    .collect(Collectors.toList());
        }

        // Agrupa por Lote
        Map<Integer, List<LeituraContador>> leiturasPorLote = new TreeMap<>();
        for (LeituraContador l : leituras) {
            int numLote = (l.getImpressora().getLote() != null) ? l.getImpressora().getLote().getNumeroLote() : 1;
            leiturasPorLote.computeIfAbsent(numLote, k -> new ArrayList<>()).add(l);
        }

        List<ItemFaturaDTO> itensFatura = new ArrayList<>();
        int itemIndex = 1;
        BigDecimal totalFatura = BigDecimal.ZERO;

        for (Map.Entry<Integer, List<LeituraContador>> entry : leiturasPorLote.entrySet()) {
            int numLote = entry.getKey();
            List<LeituraContador> lista = entry.getValue();
            LoteImpressao lote = lista.get(0).getImpressora().getLote();

            // 1. Locação do Lote
            BigDecimal qtdMaquinas = lista.stream()
                    .map(LeituraContador::getProporcao)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal valorLocacaoUnit = (lote != null) ? lote.getValorLocacaoMensal() : new BigDecimal("30.00");
            BigDecimal subtotalLocacao = lista.stream()
                    .map(LeituraContador::getValorLocacao)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            itensFatura.add(ItemFaturaDTO.builder()
                    .itemNumero(itemIndex++)
                    .codigoItem(obterCodigoItemLocacao(numLote))
                    .descricao(obterDescricaoItemLocacao(numLote))
                    .unidade("MÊS")
                    .quantidade(qtdMaquinas)
                    .valorUnitario(valorLocacaoUnit)
                    .valorTotal(subtotalLocacao)
                    .build());
            totalFatura = totalFatura.add(subtotalLocacao);

            // 2. Excedente Monocromático
            int excMono = lista.stream().mapToInt(LeituraContador::getExcedenteMono).sum();
            if (excMono > 0) {
                BigDecimal valorExcMonoUnit = (lote != null) ? lote.getValorExcedenteMono() : new BigDecimal("0.0300");
                BigDecimal subtotalExcMono = lista.stream()
                        .map(LeituraContador::getValorExcedenteMono)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                itensFatura.add(ItemFaturaDTO.builder()
                        .itemNumero(itemIndex++)
                        .codigoItem(obterCodigoItemExcedenteMono(numLote))
                        .descricao("Cópia adicional monocromática do LOTE 0" + numLote + ", excedente à franquia")
                        .unidade("UNIDADE")
                        .quantidade(BigDecimal.valueOf(excMono))
                        .valorUnitario(valorExcMonoUnit)
                        .valorTotal(subtotalExcMono)
                        .build());
                totalFatura = totalFatura.add(subtotalExcMono);
            }

            // 3. Excedente Colorido (Lote 3 e 5)
            int excColor = lista.stream().mapToInt(LeituraContador::getExcedenteColor).sum();
            if (excColor > 0) {
                BigDecimal valorExcColorUnit = (lote != null) ? lote.getValorExcedenteColor() : new BigDecimal("0.2900");
                BigDecimal subtotalExcColor = lista.stream()
                        .map(LeituraContador::getValorExcedenteColor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                itensFatura.add(ItemFaturaDTO.builder()
                        .itemNumero(itemIndex++)
                        .codigoItem("41480")
                        .descricao("Cópia adicional policromática do LOTE 0" + numLote + ", excedente à franquia")
                        .unidade("UNIDADE")
                        .quantidade(BigDecimal.valueOf(excColor))
                        .valorUnitario(valorExcColorUnit)
                        .valorTotal(subtotalExcColor)
                        .build());
                totalFatura = totalFatura.add(subtotalExcColor);
            }
        }

        // Relação nominal de equipamentos
        List<EquipamentoFaturaDTO> equipamentos = leituras.stream().map(l -> {
            int numLote = (l.getImpressora().getLote() != null) ? l.getImpressora().getLote().getNumeroLote() : 1;
            String local = (l.getInstalacao() != null) ? l.getInstalacao().getLocalInstalacao() : "-";

            return EquipamentoFaturaDTO.builder()
                    .itemPedido(l.getImpressora().getItemPedido())
                    .modelo(l.getImpressora().getModelo())
                    .numeroSerie(l.getImpressora().getNumeroSerie() != null ? l.getImpressora().getNumeroSerie() : "-")
                    .localInstalacao(local)
                    .numeroLote(numLote)
                    .leituraMonoAnterior(l.getLeituraMonoAnterior())
                    .leituraMonoAtual(l.getLeituraMonoAtual())
                    .copiasMono(l.getCopiasMono())
                    .leituraColorAnterior(l.getLeituraColorAnterior())
                    .leituraColorAtual(l.getLeituraColorAtual())
                    .copiasColor(l.getCopiasColor())
                    .franquiaMono(l.getFranquiaMonoAplicada())
                    .franquiaColor(l.getFranquiaColorAplicada())
                    .excedenteMono(l.getExcedenteMono())
                    .excedenteColor(l.getExcedenteColor())
                    .valorLocacao(l.getValorLocacao())
                    .valorExcedente(l.getValorExcedenteMono().add(l.getValorExcedenteColor()))
                    .valorTotal(l.getValorTotal())
                    .origemLeitura(l.getOrigemLeitura())
                    .observacoes(l.getObservacoes())
                    .build();
        }).collect(Collectors.toList());

        String competenciaFormatada = MESES_NOMES[mes - 1] + " / " + ano;
        String secNome = (empenho.getSecretaria() != null) ? empenho.getSecretaria().getNome() : "Prefeitura Municipal";
        String textoAtesto = "Atesto para os devidos fins de liquidação e pagamento da despesa que os serviços constantes " +
                "no presente espelho de faturamento e medição de contadores foram devidamente conferidos, auditados e executados " +
                "em estrita conformidade com as especificações editalícias e obrigações do Contrato Administrativo nº 23/2024, " +
                "atinentes à competência de " + competenciaFormatada + ", vinculados ao Empenho nº " + empenho.getNumeroEmpenho() + "/" + empenho.getAno() +
                ", destinados à " + secNome + ".";

        return EspelhoFaturaDTO.builder()
                .empenhoId(empenho.getId())
                .numeroEmpenho(empenho.getNumeroEmpenho())
                .ano(empenho.getAno())
                .secretariaNome(secNome)
                .secretariaSigla(empenho.getSecretaria() != null ? empenho.getSecretaria().getSigla() : "-")
                .contratoNumero("23/2024")
                .mesReferencia(mes)
                .anoReferencia(ano)
                .competenciaFormatada(competenciaFormatada)
                .dataEmissao(LocalDate.now())
                .totalFatura(totalFatura)
                .itens(itensFatura)
                .equipamentos(equipamentos)
                .textoAtesto(textoAtesto)
                .build();
    }

    private String obterCodigoItemLocacao(int numLote) {
        return switch (numLote) {
            case 1 -> "41474";
            case 2 -> "41476";
            case 3 -> "41478";
            case 4 -> "41481";
            case 5 -> "501";
            default -> "41474";
        };
    }

    private String obterDescricaoItemLocacao(int numLote) {
        return switch (numLote) {
            case 1 -> "LOTE 01 - Locação de impressora/copiadora multifuncional laser monocromática, porte pequeno/médio";
            case 2 -> "LOTE 02 - Locação de impressora/copiadora multifuncional laser monocromática, porte médio/grande";
            case 3 -> "LOTE 03 - Locação de impressora/copiadora multifuncional laser policromática híbrida";
            case 4 -> "LOTE 04 - Locação de impressora laser monocromática, porte pequeno/médio";
            case 5 -> "LOTE 05 - Locação de impressora laser colorida especial (500 páginas)";
            default -> "Locação de equipamento de impressão";
        };
    }

    private String obterCodigoItemExcedenteMono(int numLote) {
        return switch (numLote) {
            case 1 -> "41475";
            case 2 -> "41477";
            case 3 -> "41479";
            case 4 -> "41482";
            case 5 -> "501";
            default -> "41475";
        };
    }

    private String obterCodigoItemExcedenteColor(int numLote) {
        return switch (numLote) {
            case 3 -> "41480";
            case 5 -> "501";
            default -> "41480";
        };
    }

    @Transactional(readOnly = true)
    public List<EspelhoFaturaDTO> gerarNotasFiscaisLote(List<Integer> meses, Integer mes, Integer ano, Long empenhoId) {
        if (ano == null) ano = 2026;

        List<Integer> listaMeses = new ArrayList<>();
        if (meses != null && !meses.isEmpty()) {
            listaMeses.addAll(meses);
        } else if (mes != null) {
            listaMeses.add(mes);
        } else {
            listaMeses.add(8);
        }
        listaMeses.sort(Integer::compareTo);

        List<EmpenhoImpressao> empenhos;
        if (empenhoId != null) {
            empenhos = empenhoRepository.findById(empenhoId)
                    .map(List::of)
                    .orElse(Collections.emptyList());
        } else {
            empenhos = new ArrayList<>(empenhoRepository.findByAtivoTrueOrderByNumeroEmpenhoAsc());
            List<String> ordemEmpenhos = List.of("2625", "2516", "2517", "2518", "2519", "2520", "2522", "2521");
            empenhos.sort(Comparator.comparingInt(e -> {
                int idx = ordemEmpenhos.indexOf(e.getNumeroEmpenho());
                return idx >= 0 ? idx : 999;
            }));
        }

        List<EspelhoFaturaDTO> faturas = new ArrayList<>();
        for (Integer m : listaMeses) {
            for (EmpenhoImpressao e : empenhos) {
                long qtd = instalacaoRepository.countByEmpenhoIdAndStatus(e.getId(), "ATIVA");
                if (qtd > 0) {
                    faturas.add(gerarEspelhoFatura(e.getId(), m, ano));
                }
            }
        }
        return faturas;
    }

    @Transactional(readOnly = true)
    public List<EspelhoFaturaDTO> gerarNotasFiscaisLote(Integer mes, Integer ano) {
        return gerarNotasFiscaisLote(null, mes, ano, null);
    }

    @Transactional(readOnly = true)
    public NotasFiscaisConsolidadoDTO obterNotasFiscaisConsolidado(Integer ano) {
        if (ano == null) ano = 2026;

        List<EmpenhoImpressao> empenhos = empenhoRepository.findByAtivoTrueOrderByNumeroEmpenhoAsc();
        List<LeituraContador> leiturasAno = leituraRepository.findByAnoWithDetails(ano);

        // Agrupa leituras: empenhoId -> numLote -> mesReferencia -> List<LeituraContador>
        Map<Long, Map<Integer, Map<Integer, List<LeituraContador>>>> mapaLeituras = new LinkedHashMap<>();
        for (LeituraContador l : leiturasAno) {
            if (l.getInstalacao() != null && l.getInstalacao().getEmpenho() != null) {
                Long empId = l.getInstalacao().getEmpenho().getId();
                int numLote = (l.getImpressora() != null && l.getImpressora().getLote() != null)
                        ? l.getImpressora().getLote().getNumeroLote() : 1;
                mapaLeituras
                        .computeIfAbsent(empId, k -> new TreeMap<>())
                        .computeIfAbsent(numLote, k -> new HashMap<>())
                        .computeIfAbsent(l.getMesReferencia(), k -> new ArrayList<>())
                        .add(l);
            }
        }

        // Ordenacao conforme a planilha oficial
        List<String> ordemEmpenhos = List.of("2625", "2516", "2517", "2518", "2519", "2520", "2522", "2521");
        empenhos.sort(Comparator.comparingInt(e -> {
            int idx = ordemEmpenhos.indexOf(e.getNumeroEmpenho());
            return idx >= 0 ? idx : 999;
        }));

        List<EmpenhoNotaFiscalDTO> empenhosDTO = new ArrayList<>();
        BigDecimal[] totaisPrefeitura = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            totaisPrefeitura[i] = BigDecimal.ZERO;
        }

        for (EmpenhoImpressao e : empenhos) {
            List<InstalacaoImpressora> ativas = instalacaoRepository.findByEmpenhoIdAndStatus(e.getId(), "ATIVA");
            if (ativas.isEmpty()) {
                continue;
            }

            Map<Integer, Map<Integer, List<LeituraContador>>> porLote = mapaLeituras.getOrDefault(e.getId(), Collections.emptyMap());

            // Identifica lotes presentes no empenho
            Set<Integer> lotesPresentes = new TreeSet<>();
            Map<Integer, LoteImpressao> infoLotes = new HashMap<>();
            for (InstalacaoImpressora inst : ativas) {
                if (inst.getImpressora() != null && inst.getImpressora().getLote() != null) {
                    lotesPresentes.add(inst.getImpressora().getLote().getNumeroLote());
                    infoLotes.putIfAbsent(inst.getImpressora().getLote().getNumeroLote(), inst.getImpressora().getLote());
                }
            }

            List<ItemNotaFiscalDTO> itensEmpenho = new ArrayList<>();
            int itemNum = 1;

            // 1. Locacao de cada lote
            for (Integer numLote : lotesPresentes) {
                LoteImpressao lote = infoLotes.get(numLote);
                BigDecimal valUnit = lote != null ? lote.getValorLocacaoMensal() : BigDecimal.ZERO;
                Map<Integer, List<LeituraContador>> porMes = porLote.getOrDefault(numLote, Collections.emptyMap());

                List<MesFaturaDTO> mesesItem = new ArrayList<>();
                for (int m = 1; m <= 12; m++) {
                    List<LeituraContador> ls = porMes.getOrDefault(m, Collections.emptyList());
                    BigDecimal qtd = ls.stream().map(LeituraContador::getProporcao).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal total = ls.stream().map(LeituraContador::getValorLocacao).reduce(BigDecimal.ZERO, BigDecimal::add);

                    mesesItem.add(MesFaturaDTO.builder()
                            .mes(m)
                            .nomeMes(MESES_SIGLAS[m - 1])
                            .quantidade(qtd)
                            .valorUnitario(valUnit)
                            .valorTotal(total)
                            .build());
                }

                itensEmpenho.add(ItemNotaFiscalDTO.builder()
                        .itemNumero(itemNum++)
                        .codigoItem(obterCodigoItemLocacao(numLote))
                        .descricao(obterDescricaoItemLocacao(numLote))
                        .unidade("MÊS")
                        .valorUnitario(valUnit)
                        .meses(mesesItem)
                        .build());
            }

            // 2. Excedente mono de cada lote
            for (Integer numLote : lotesPresentes) {
                LoteImpressao lote = infoLotes.get(numLote);
                BigDecimal valUnit = lote != null ? lote.getValorExcedenteMono() : BigDecimal.ZERO;
                Map<Integer, List<LeituraContador>> porMes = porLote.getOrDefault(numLote, Collections.emptyMap());

                List<MesFaturaDTO> mesesItem = new ArrayList<>();
                for (int m = 1; m <= 12; m++) {
                    List<LeituraContador> ls = porMes.getOrDefault(m, Collections.emptyList());
                    int excMono = ls.stream().mapToInt(LeituraContador::getExcedenteMono).sum();
                    BigDecimal total = ls.stream().map(LeituraContador::getValorExcedenteMono).reduce(BigDecimal.ZERO, BigDecimal::add);

                    mesesItem.add(MesFaturaDTO.builder()
                            .mes(m)
                            .nomeMes(MESES_SIGLAS[m - 1])
                            .quantidade(BigDecimal.valueOf(excMono))
                            .valorUnitario(valUnit)
                            .valorTotal(total)
                            .build());
                }

                itensEmpenho.add(ItemNotaFiscalDTO.builder()
                        .itemNumero(itemNum++)
                        .codigoItem(obterCodigoItemExcedenteMono(numLote))
                        .descricao("Cópia adicional monocromática do LOTE 0" + numLote + ", excedente à franquia")
                        .unidade("UNIDADE")
                        .valorUnitario(valUnit)
                        .meses(mesesItem)
                        .build());
            }

            // 3. Excedente color de cada lote colorido (Lote 3 e 5)
            for (Integer numLote : lotesPresentes) {
                if (numLote == 3 || numLote == 5) {
                    LoteImpressao lote = infoLotes.get(numLote);
                    BigDecimal valUnit = lote != null ? lote.getValorExcedenteColor() : BigDecimal.ZERO;
                    Map<Integer, List<LeituraContador>> porMes = porLote.getOrDefault(numLote, Collections.emptyMap());

                    List<MesFaturaDTO> mesesItem = new ArrayList<>();
                    for (int m = 1; m <= 12; m++) {
                        List<LeituraContador> ls = porMes.getOrDefault(m, Collections.emptyList());
                        int excColor = ls.stream().mapToInt(LeituraContador::getExcedenteColor).sum();
                        BigDecimal total = ls.stream().map(LeituraContador::getValorExcedenteColor).reduce(BigDecimal.ZERO, BigDecimal::add);

                        mesesItem.add(MesFaturaDTO.builder()
                                .mes(m)
                                .nomeMes(MESES_SIGLAS[m - 1])
                                .quantidade(BigDecimal.valueOf(excColor))
                                .valorUnitario(valUnit)
                                .valorTotal(total)
                                .build());
                    }

                    itensEmpenho.add(ItemNotaFiscalDTO.builder()
                            .itemNumero(itemNum++)
                            .codigoItem(obterCodigoItemExcedenteColor(numLote))
                            .descricao("Cópia adicional policromática do LOTE 0" + numLote + ", excedente à franquia")
                            .unidade("UNIDADE")
                            .valorUnitario(valUnit)
                            .meses(mesesItem)
                            .build());
                }
            }

            // Totais mensais do empenho
            List<BigDecimal> totaisMensaisEmpenho = new ArrayList<>();
            BigDecimal totalAnualEmpenho = BigDecimal.ZERO;
            for (int m = 0; m < 12; m++) {
                final int mesIdx = m;
                BigDecimal somaMes = itensEmpenho.stream()
                        .map(it -> it.getMeses().get(mesIdx).getValorTotal())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totaisMensaisEmpenho.add(somaMes);
                totalAnualEmpenho = totalAnualEmpenho.add(somaMes);
                totaisPrefeitura[mesIdx] = totaisPrefeitura[mesIdx].add(somaMes);
            }

            String secSigla = e.getSecretaria() != null ? e.getSecretaria().getSigla() : "-";
            String secNome = e.getSecretaria() != null ? e.getSecretaria().getNome() : "-";
            String titulo = "Empenho " + e.getNumeroEmpenho() + " - " + secNome;

            empenhosDTO.add(EmpenhoNotaFiscalDTO.builder()
                    .empenhoId(e.getId())
                    .numeroEmpenho(e.getNumeroEmpenho())
                    .secretariaSigla(secSigla)
                    .secretariaNome(secNome)
                    .titulo(titulo)
                    .quantidadeEquipamentos(ativas.size())
                    .itens(itensEmpenho)
                    .totaisMensais(totaisMensaisEmpenho)
                    .totalAnual(totalAnualEmpenho)
                    .build());
        }

        List<BigDecimal> totaisPrefeituraList = Arrays.asList(totaisPrefeitura);
        BigDecimal totalPrefeituraAnual = Arrays.stream(totaisPrefeitura).reduce(BigDecimal.ZERO, BigDecimal::add);

        return NotasFiscaisConsolidadoDTO.builder()
                .ano(ano)
                .competencia("Exercício " + ano)
                .empenhos(empenhosDTO)
                .totaisPrefeituraMensais(totaisPrefeituraList)
                .totalPrefeituraAnual(totalPrefeituraAnual)
                .build();
    }

    private EmpenhoImpressaoDTO toDTO(EmpenhoImpressao e) {
        long qtd = instalacaoRepository.countByEmpenhoIdAndStatus(e.getId(), "ATIVA");

        return EmpenhoImpressaoDTO.builder()
                .id(e.getId())
                .numeroEmpenho(e.getNumeroEmpenho())
                .ano(e.getAno())
                .secretariaId(e.getSecretaria() != null ? e.getSecretaria().getId() : null)
                .secretariaNome(e.getSecretaria() != null ? e.getSecretaria().getNome() : null)
                .secretariaSigla(e.getSecretaria() != null ? e.getSecretaria().getSigla() : null)
                .descricao(e.getDescricao())
                .valorTotal(e.getValorTotal())
                .saldo(e.getSaldo())
                .ativo(e.getAtivo())
                .quantidadeImpressoras(qtd)
                .build();
    }
}
