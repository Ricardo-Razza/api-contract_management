package com.contract_management.api.modules.impressora.service.financeiro;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.impressora.dto.request.EmpenhoRequestDTO;
import com.contract_management.api.modules.impressora.dto.response.EmpenhoImpressaoDTO;
import com.contract_management.api.modules.impressora.dto.response.EspelhoFaturaDTO;
import com.contract_management.api.modules.impressora.dto.response.ExecucaoMensalDTO;
import com.contract_management.api.modules.impressora.dto.response.NotasFiscaisConsolidadoDTO;
import com.contract_management.api.modules.impressora.model.EmpenhoImpressao;
import com.contract_management.api.modules.impressora.model.Impressora;
import com.contract_management.api.modules.impressora.model.InstalacaoImpressora;
import com.contract_management.api.modules.impressora.model.LeituraContador;
import com.contract_management.api.modules.impressora.model.LoteImpressao;
import com.contract_management.api.modules.impressora.repository.EmpenhoImpressaoRepository;
import com.contract_management.api.modules.impressora.repository.ImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.LeituraContadorRepository;
import com.contract_management.api.modules.impressora.repository.LoteImpressaoRepository;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.api.SecretariaConsulta;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpenhoImpressaoService {

    private final EmpenhoImpressaoRepository empenhoRepository;
    private final InstalacaoImpressoraRepository instalacaoRepository;
    private final SecretariaConsulta secretariaRepository;
    private final ExecucaoOrcamentariaService execucaoOrcamentariaService;
    private final FaturamentoImpressaoService faturamentoImpressaoService;
    private final LeituraContadorRepository leituraRepository;
    private final LoteImpressaoRepository loteRepository;
    private final ImpressoraRepository impressoraRepository;

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
        return execucaoOrcamentariaService.obterExecucaoMensal(ano);
    }

    @Transactional(readOnly = true)
    public EspelhoFaturaDTO gerarEspelhoFatura(Long empenhoId, Integer mes, Integer ano) {
        return faturamentoImpressaoService.gerarEspelhoFatura(empenhoId, mes, ano);
    }

    @Transactional(readOnly = true)
    public List<EspelhoFaturaDTO> gerarNotasFiscaisLote(List<Integer> meses, Integer mes, Integer ano, Long empenhoId) {
        return faturamentoImpressaoService.gerarNotasFiscaisLote(meses, mes, ano, empenhoId);
    }

    @Transactional(readOnly = true)
    public List<EspelhoFaturaDTO> gerarNotasFiscaisLote(Integer mes, Integer ano) {
        return faturamentoImpressaoService.gerarNotasFiscaisLote(mes, ano);
    }

    @Transactional(readOnly = true)
    public NotasFiscaisConsolidadoDTO obterNotasFiscaisConsolidado(Integer ano) {
        return faturamentoImpressaoService.obterNotasFiscaisConsolidado(ano);
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