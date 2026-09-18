package com.contract_management.api.service;

import com.contract_management.api.dto.response.EmpenhoImpressaoDTO;
import com.contract_management.api.model.EmpenhoImpressao;
import com.contract_management.api.repository.EmpenhoImpressaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmpenhoImpressaoService {

    private final EmpenhoImpressaoRepository empenhoRepository;

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

    private EmpenhoImpressaoDTO toDTO(EmpenhoImpressao e) {
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
                .build();
    }
}
