package com.contract_management.api.modules.impressora.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.impressora.dto.request.ImpressoraRequestDTO;
import com.contract_management.api.modules.impressora.dto.request.SubstituicaoImpressoraRequestDTO;
import com.contract_management.api.modules.impressora.dto.request.TrocaLocalRequestDTO;
import com.contract_management.api.modules.impressora.dto.response.ImpressoraResponseDTO;
import com.contract_management.api.modules.impressora.model.EmpenhoImpressao;
import com.contract_management.api.modules.impressora.model.Impressora;
import com.contract_management.api.modules.impressora.model.InstalacaoImpressora;
import com.contract_management.api.modules.impressora.model.LeituraContador;
import com.contract_management.api.modules.impressora.model.LocalInstalacao;
import com.contract_management.api.modules.impressora.model.LoteImpressao;
import com.contract_management.api.modules.impressora.repository.EmpenhoImpressaoRepository;
import com.contract_management.api.modules.impressora.repository.ImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.LeituraContadorRepository;
import com.contract_management.api.modules.impressora.repository.LocalInstalacaoRepository;
import com.contract_management.api.modules.impressora.repository.LoteImpressaoRepository;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.repository.SecretariaRepository;

@Service
@RequiredArgsConstructor
public class ImpressoraService {

    private final ImpressoraRepository impressoraRepository;
    private final InstalacaoImpressoraRepository instalacaoRepository;
    private final LoteImpressaoRepository loteRepository;
    private final SecretariaRepository secretariaRepository;
    private final EmpenhoImpressaoRepository empenhoRepository;
    private final LeituraContadorRepository leituraRepository;
    private final LocalInstalacaoRepository localRepository;

    @Transactional(readOnly = true)
    public List<ImpressoraResponseDTO> listarTodas() {
        List<InstalacaoImpressora> instalacoesAtivas = instalacaoRepository.findAllAtivasWithDetails();
        return instalacoesAtivas.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ImpressoraResponseDTO buscarPorId(Long id) {
        Impressora impressora = impressoraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Impressora", id));
        InstalacaoImpressora instalacao = instalacaoRepository.findAtivaByImpressoraId(id)
                .orElse(null);
        return toResponseDTO(impressora, instalacao);
    }

    @Transactional(readOnly = true)
    public List<com.contract_management.api.modules.impressora.dto.response.InstalacaoHistoricoDTO> historico(Long id) {
        if (!impressoraRepository.existsById(id)) throw new EntityNotFoundException("Impressora", id);
        return instalacaoRepository.findByImpressoraIdOrderByDataInstalacaoDesc(id).stream()
                .map(com.contract_management.api.modules.impressora.dto.response.InstalacaoHistoricoDTO::from).toList();
    }

    private LocalInstalacao validarLocal(Long id, Long secretariaId) {
        if (id == null) throw new com.contract_management.api.common.exception.BusinessException("Selecione um local cadastrado.");
        LocalInstalacao local = localRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Local de Instalação", id));
        if (!Boolean.TRUE.equals(local.getAtivo()) || !local.getSecretaria().getId().equals(secretariaId)) {
            throw new com.contract_management.api.common.exception.BusinessException("O local deve estar ativo e pertencer à secretaria selecionada.");
        }
        return local;
    }

    @Transactional
    public ImpressoraResponseDTO criar(ImpressoraRequestDTO dto) {
        LocalInstalacao localSelecionado = validarLocal(dto.getLocalInstalacaoId(), dto.getSecretariaId());
        LoteImpressao lote = null;
        if (dto.getLoteId() != null) {
            lote = loteRepository.findById(dto.getLoteId()).orElse(null);
        }

        Secretaria secretaria = secretariaRepository.findById(dto.getSecretariaId())
                .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getSecretariaId()));

        EmpenhoImpressao empenho = null;
        if (dto.getEmpenhoId() != null) {
            empenho = empenhoRepository.findById(dto.getEmpenhoId()).orElse(null);
        }

        Impressora impressora = Impressora.builder()
                .itemPedido(dto.getItemPedido())
                .numeroSerie(dto.getNumeroSerie())
                .fabricante(dto.getFabricante())
                .modelo(dto.getModelo())
                .tipoImpressao(dto.getTipoImpressao() != null ? dto.getTipoImpressao() : "MONO")
                .lote(lote)
                .ip(dto.getIp())
                .ativo(true)
                .build();

        Impressora salva = impressoraRepository.save(impressora);

        String localNome = localSelecionado.getNome();
        String endereco = dto.getEndereco();
        String responsavel = dto.getResponsavel();

        if (dto.getLocalInstalacaoId() != null) {
            LocalInstalacao local = localRepository.findById(dto.getLocalInstalacaoId()).orElse(null);
            if (local != null) {
                if (localNome == null || localNome.isBlank()) localNome = local.getNome();
                if (endereco == null || endereco.isBlank()) endereco = local.getEndereco();
                if (responsavel == null || responsavel.isBlank()) responsavel = local.getResponsavel();
            }
        }

        InstalacaoImpressora instalacao = InstalacaoImpressora.builder()
                .impressora(salva)
                .secretaria(secretaria)
                .empenho(empenho)
                .localInstalacao(localNome)
                .localInstalacaoId(localSelecionado.getId())
                .endereco(endereco)
                .responsavel(responsavel)
                .transformador(dto.getTransformador())
                .dataInstalacao(dto.getDataInstalacao() != null ? dto.getDataInstalacao() : LocalDate.now())
                .contadorInstalacaoMono(dto.getContadorInicialMono() != null ? dto.getContadorInicialMono() : 0)
                .contadorInstalacaoColor(dto.getContadorInicialColor() != null ? dto.getContadorInicialColor() : 0)
                .status("ATIVA")
                .build();

        instalacaoRepository.save(instalacao);

        return toResponseDTO(salva, instalacao);
    }

    @Transactional
    public ImpressoraResponseDTO atualizar(Long id, ImpressoraRequestDTO dto) {
        LocalInstalacao localSelecionado = validarLocal(dto.getLocalInstalacaoId(), dto.getSecretariaId());
        Impressora impressora = impressoraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Impressora", id));

        if (dto.getLoteId() != null) {
            LoteImpressao lote = loteRepository.findById(dto.getLoteId()).orElse(null);
            impressora.setLote(lote);
        }

        impressora.setItemPedido(dto.getItemPedido());
        impressora.setNumeroSerie(dto.getNumeroSerie());
        impressora.setFabricante(dto.getFabricante());
        impressora.setModelo(dto.getModelo());
        impressora.setTipoImpressao(dto.getTipoImpressao());
        impressora.setIp(dto.getIp());

        impressoraRepository.save(impressora);

        InstalacaoImpressora instalacao = instalacaoRepository.findAtivaByImpressoraId(id).orElse(null);
        if (instalacao != null) {
            if (!localSelecionado.getId().equals(instalacao.getLocalInstalacaoId())) {
                throw new com.contract_management.api.common.exception.BusinessException("Use o remanejamento para alterar o local e preservar o histórico.");
            }
            if (dto.getSecretariaId() != null && !dto.getSecretariaId().equals(instalacao.getSecretaria().getId())) {
                Secretaria sec = secretariaRepository.findById(dto.getSecretariaId()).orElse(null);
                if (sec != null) instalacao.setSecretaria(sec);
            }
            if (dto.getEmpenhoId() != null) {
                EmpenhoImpressao emp = empenhoRepository.findById(dto.getEmpenhoId()).orElse(null);
                instalacao.setEmpenho(emp);
            }
            instalacao.setLocalInstalacao(localSelecionado.getNome());
            instalacao.setEndereco(dto.getEndereco());
            instalacao.setResponsavel(dto.getResponsavel());
            instalacao.setTransformador(dto.getTransformador());
            instalacaoRepository.save(instalacao);
        }

        return toResponseDTO(impressora, instalacao);
    }

    @Transactional
    public ImpressoraResponseDTO remanejarLocal(Long id, TrocaLocalRequestDTO dto) {
        LocalInstalacao localSelecionado = validarLocal(dto.getLocalInstalacaoId(), dto.getNovaSecretariaId());
        Impressora impressora = impressoraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Impressora", id));

        InstalacaoImpressora instalacaoAtual = instalacaoRepository.findAtivaByImpressoraId(id)
                .orElseThrow(() -> new IllegalStateException("Impressora não possui instalação ativa"));

        LocalDate dataMudanca = dto.getDataMudanca() != null ? dto.getDataMudanca() : LocalDate.now();

        if (dataMudanca.isBefore(instalacaoAtual.getDataInstalacao())) {
            throw new com.contract_management.api.common.exception.BusinessException("A mudança não pode ser anterior à instalação atual.");
        }
        if (localSelecionado.getId().equals(instalacaoAtual.getLocalInstalacaoId())) {
            throw new com.contract_management.api.common.exception.BusinessException("Selecione um local diferente do atual.");
        }

        // 1. Encerra a instalação antiga
        instalacaoAtual.setDataRetirada(dataMudanca);
        instalacaoAtual.setContadorRetiradaMono(dto.getContadorAtualMono());
        instalacaoAtual.setContadorRetiradaColor(dto.getContadorAtualColor());
        instalacaoAtual.setMotivoRetirada(dto.getMotivo() != null ? dto.getMotivo() : "Remanejamento de setor");
        instalacaoAtual.setStatus("REMANEJADA");
        instalacaoRepository.save(instalacaoAtual);

        // 2. Resolve a nova Secretaria, Local e Endereço
        Secretaria novaSecretaria = null;
        String novoLocalNome = localSelecionado.getNome();
        String novoEndereco = dto.getNovoEndereco();
        String novoResponsavel = dto.getNovoResponsavel();

        if (dto.getLocalInstalacaoId() != null) {
            LocalInstalacao local = localRepository.findById(dto.getLocalInstalacaoId()).orElse(null);
            if (local != null) {
                novaSecretaria = local.getSecretaria();
                if (novoLocalNome == null || novoLocalNome.isBlank()) novoLocalNome = local.getNome();
                if (novoEndereco == null || novoEndereco.isBlank()) novoEndereco = local.getEndereco();
                if (novoResponsavel == null || novoResponsavel.isBlank()) novoResponsavel = local.getResponsavel();
            }
        }

        if (novaSecretaria == null && dto.getNovaSecretariaId() != null) {
            novaSecretaria = secretariaRepository.findById(dto.getNovaSecretariaId())
                    .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getNovaSecretariaId()));
        }

        if (novaSecretaria == null) {
            novaSecretaria = instalacaoAtual.getSecretaria();
        }

        // Se mudou de secretaria, vincula ao empenho ativo da nova secretaria
        EmpenhoImpressao novoEmpenho = empenhoRepository.findBySecretariaIdAndAtivoTrue(novaSecretaria.getId())
                .stream().findFirst().orElse(instalacaoAtual.getEmpenho());

        InstalacaoImpressora novaInstalacao = InstalacaoImpressora.builder()
                .impressora(impressora)
                .secretaria(novaSecretaria)
                .empenho(novoEmpenho)
                .localInstalacao(novoLocalNome)
                .localInstalacaoId(localSelecionado.getId())
                .endereco(novoEndereco != null ? novoEndereco : instalacaoAtual.getEndereco())
                .responsavel(novoResponsavel != null ? novoResponsavel : instalacaoAtual.getResponsavel())
                .transformador(dto.getNovoTransformador() != null ? dto.getNovoTransformador() : instalacaoAtual.getTransformador())
                .dataInstalacao(dataMudanca)
                .contadorInstalacaoMono(dto.getContadorAtualMono() != null ? dto.getContadorAtualMono() : 0)
                .contadorInstalacaoColor(dto.getContadorAtualColor() != null ? dto.getContadorAtualColor() : 0)
                .status("ATIVA")
                .build();

        instalacaoRepository.save(novaInstalacao);

        if (dto.getNovoIp() != null && !dto.getNovoIp().isBlank()) {
            impressora.setIp(dto.getNovoIp());
            impressoraRepository.save(impressora);
        }

        return toResponseDTO(impressora, novaInstalacao);
    }

    @Transactional
    public ImpressoraResponseDTO substituirPorDefeito(Long id, SubstituicaoImpressoraRequestDTO dto) {
        Impressora impressoraAntiga = impressoraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Impressora", id));

        InstalacaoImpressora instalacaoAtual = instalacaoRepository.findAtivaByImpressoraId(id)
                .orElseThrow(() -> new IllegalStateException("Impressora não possui instalação ativa"));

        LocalDate dataSub = dto.getDataSubstituicao() != null ? dto.getDataSubstituicao() : LocalDate.now();
        int mesSub = dataSub.getMonthValue();
        int anoSub = dataSub.getYear();

        // 1. Encerra instalação da máquina com defeito
        instalacaoAtual.setDataRetirada(dataSub);
        instalacaoAtual.setContadorRetiradaMono(dto.getContadorFinalMonoRetirada());
        instalacaoAtual.setContadorRetiradaColor(dto.getContadorFinalColorRetirada());
        instalacaoAtual.setMotivoRetirada("Defeito: " + dto.getMotivoDefeito());
        instalacaoAtual.setStatus("SUBSTITUIDA");
        instalacaoRepository.save(instalacaoAtual);

        impressoraAntiga.setAtivo(false);
        impressoraRepository.save(impressoraAntiga);

        // 2. Registra automaticamente a medição parcial proporcional da máquina retirada (origem SWAP_RETIRADA)
        List<LeituraContador> anterioresAntiga = leituraRepository.findUltimasLeiturasPorImpressora(impressoraAntiga.getId());
        int leituraMonoAnterior = instalacaoAtual.getContadorInstalacaoMono() != null ? instalacaoAtual.getContadorInstalacaoMono() : 0;
        int leituraColorAnterior = instalacaoAtual.getContadorInstalacaoColor() != null ? instalacaoAtual.getContadorInstalacaoColor() : 0;

        for (LeituraContador ant : anterioresAntiga) {
            if (ant.getAnoReferencia() < anoSub || (ant.getAnoReferencia().equals(anoSub) && ant.getMesReferencia() < mesSub)) {
                leituraMonoAnterior = ant.getLeituraMonoAtual() != null ? ant.getLeituraMonoAtual() : 0;
                leituraColorAnterior = ant.getLeituraColorAtual() != null ? ant.getLeituraColorAtual() : 0;
                break;
            }
        }

        int finalMono = dto.getContadorFinalMonoRetirada() != null ? dto.getContadorFinalMonoRetirada() : leituraMonoAnterior;
        int finalColor = dto.getContadorFinalColorRetirada() != null ? dto.getContadorFinalColorRetirada() : leituraColorAnterior;
        int copiasMonoAntiga = Math.max(0, finalMono - leituraMonoAnterior);
        int copiasColorAntiga = Math.max(0, finalColor - leituraColorAnterior);

        LeituraContador leituraSwapRetirada = leituraRepository
                .findByImpressoraIdAndMesReferenciaAndAnoReferencia(impressoraAntiga.getId(), mesSub, anoSub)
                .orElse(LeituraContador.builder()
                        .impressora(impressoraAntiga)
                        .instalacao(instalacaoAtual)
                        .mesReferencia(mesSub)
                        .anoReferencia(anoSub)
                        .build());

        leituraSwapRetirada.setInstalacao(instalacaoAtual);
        leituraSwapRetirada.setDataLeitura(dataSub);
        leituraSwapRetirada.setLeituraMonoAnterior(leituraMonoAnterior);
        leituraSwapRetirada.setLeituraMonoAtual(finalMono);
        leituraSwapRetirada.setCopiasMono(copiasMonoAntiga);
        leituraSwapRetirada.setLeituraColorAnterior(leituraColorAnterior);
        leituraSwapRetirada.setLeituraColorAtual(finalColor);
        leituraSwapRetirada.setCopiasColor(copiasColorAntiga);
        leituraSwapRetirada.setProporcao(BigDecimal.ZERO);
        leituraSwapRetirada.setFranquiaMonoAplicada(0);
        leituraSwapRetirada.setFranquiaColorAplicada(0);
        leituraSwapRetirada.setExcedenteMono(0);
        leituraSwapRetirada.setExcedenteColor(0);
        leituraSwapRetirada.setValorLocacao(BigDecimal.ZERO);
        leituraSwapRetirada.setValorExcedenteMono(BigDecimal.ZERO);
        leituraSwapRetirada.setValorExcedenteColor(BigDecimal.ZERO);
        leituraSwapRetirada.setValorTotal(BigDecimal.ZERO);
        leituraSwapRetirada.setOrigemLeitura("SWAP_RETIRADA");
        leituraSwapRetirada.setObservacoes("Substituída em " + dataSub + " por defeito: " + dto.getMotivoDefeito() +
                ". Cópias parciais (" + copiasMonoAntiga + " mono) consolidadas na máquina substituta.");

        leituraRepository.save(leituraSwapRetirada);

        // 3. Cadastra a nova impressora substituta
        Impressora novaImpressora = Impressora.builder()
                .itemPedido(impressoraAntiga.getItemPedido())
                .numeroSerie(dto.getNovoNumeroSerie())
                .fabricante(dto.getNovoFabricante() != null ? dto.getNovoFabricante() : impressoraAntiga.getFabricante())
                .modelo(dto.getNovoModelo())
                .tipoImpressao(impressoraAntiga.getTipoImpressao())
                .lote(impressoraAntiga.getLote())
                .ip(impressoraAntiga.getIp())
                .ativo(true)
                .build();

        Impressora novaSalva = impressoraRepository.save(novaImpressora);

        // 4. Instala a nova impressora no mesmo local
        InstalacaoImpressora novaInstalacao = InstalacaoImpressora.builder()
                .impressora(novaSalva)
                .secretaria(instalacaoAtual.getSecretaria())
                .empenho(instalacaoAtual.getEmpenho())
                .localInstalacao(instalacaoAtual.getLocalInstalacao())
                .localInstalacaoId(instalacaoAtual.getLocalInstalacaoId())
                .endereco(instalacaoAtual.getEndereco())
                .responsavel(instalacaoAtual.getResponsavel())
                .transformador(instalacaoAtual.getTransformador())
                .dataInstalacao(dataSub)
                .contadorInstalacaoMono(dto.getContadorInicialMonoNova())
                .contadorInstalacaoColor(dto.getContadorInicialColorNova() != null ? dto.getContadorInicialColorNova() : 0)
                .status("ATIVA")
                .build();

        instalacaoRepository.save(novaInstalacao);

        return toResponseDTO(novaSalva, novaInstalacao);
    }

    @Transactional
    public void excluir(Long id) {
        Impressora impressora = impressoraRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Impressora", id));
        impressora.setAtivo(false);
        impressoraRepository.save(impressora);

        instalacaoRepository.findAtivaByImpressoraId(id).ifPresent(inst -> {
            inst.setStatus("RECOLHIDA");
            inst.setDataRetirada(LocalDate.now());
            instalacaoRepository.save(inst);
        });
    }

    private ImpressoraResponseDTO toResponseDTO(InstalacaoImpressora inst) {
        return toResponseDTO(inst.getImpressora(), inst);
    }

    private ImpressoraResponseDTO toResponseDTO(Impressora imp, InstalacaoImpressora inst) {
        ImpressoraResponseDTO.ImpressoraResponseDTOBuilder builder = ImpressoraResponseDTO.builder()
                .id(imp.getId())
                .itemPedido(imp.getItemPedido())
                .numeroSerie(imp.getNumeroSerie())
                .fabricante(imp.getFabricante())
                .modelo(imp.getModelo())
                .tipoImpressao(imp.getTipoImpressao())
                .ip(imp.getIp())
                .ativo(imp.getAtivo());

        if (imp.getLote() != null) {
            LoteImpressao lote = imp.getLote();
            builder.loteId(lote.getId())
                    .numeroLote(lote.getNumeroLote())
                    .loteDescricao(lote.getDescricao())
                    .franquiaMono(lote.getFranquiaMono())
                    .franquiaColor(lote.getFranquiaColor())
                    .valorLocacaoMensal(lote.getValorLocacaoMensal())
                    .valorExcedenteMono(lote.getValorExcedenteMono())
                    .valorExcedenteColor(lote.getValorExcedenteColor());
        }

        if (inst != null) {
            builder.instalacaoId(inst.getId())
                    .localInstalacao(inst.getLocalInstalacao())
                    .localInstalacaoId(inst.getLocalInstalacaoId())
                    .endereco(inst.getEndereco())
                    .responsavel(inst.getResponsavel())
                    .transformador(inst.getTransformador())
                    .dataInstalacao(inst.getDataInstalacao())
                    .contadorInstalacaoMono(inst.getContadorInstalacaoMono())
                    .contadorInstalacaoColor(inst.getContadorInstalacaoColor())
                    .statusInstalacao(inst.getStatus());

            if (inst.getSecretaria() != null) {
                builder.secretariaId(inst.getSecretaria().getId())
                        .secretariaNome(inst.getSecretaria().getNome())
                        .secretariaSigla(inst.getSecretaria().getSigla());
            }

            if (inst.getEmpenho() != null) {
                builder.empenhoId(inst.getEmpenho().getId())
                        .numeroEmpenho(inst.getEmpenho().getNumeroEmpenho());
            }
        }

        List<LeituraContador> ultimas = leituraRepository.findUltimasLeiturasPorImpressora(imp.getId());
        if (!ultimas.isEmpty()) {
            LeituraContador u = ultimas.get(0);
            builder.ultimoContadorMono(u.getLeituraMonoAtual())
                    .ultimoContadorColor(u.getLeituraColorAtual())
                    .dataUltimaLeitura(u.getDataLeitura());
        } else if (inst != null) {
            builder.ultimoContadorMono(inst.getContadorInstalacaoMono())
                    .ultimoContadorColor(inst.getContadorInstalacaoColor())
                    .dataUltimaLeitura(inst.getDataInstalacao());
        }

        return builder.build();
    }
}