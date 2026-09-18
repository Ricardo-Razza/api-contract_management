package com.contract_management.api.service;

import com.contract_management.api.dto.request.EmpenhoRequestDTO;
import com.contract_management.api.dto.response.EmpenhoImpressaoDTO;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.EmpenhoImpressao;
import com.contract_management.api.model.Secretaria;
import com.contract_management.api.repository.EmpenhoImpressaoRepository;
import com.contract_management.api.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.repository.SecretariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpenhoImpressaoService {

    private final EmpenhoImpressaoRepository empenhoRepository;
    private final InstalacaoImpressoraRepository instalacaoRepository;
    private final SecretariaRepository secretariaRepository;

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
