package com.contract_management.api.service;

import com.contract_management.api.dto.response.EmpenhoImpressaoDTO;
import com.contract_management.api.dto.response.LoteImpressaoDTO;
import com.contract_management.api.model.EmpenhoImpressao;
import com.contract_management.api.model.LoteImpressao;
import com.contract_management.api.repository.EmpenhoImpressaoRepository;
import com.contract_management.api.repository.LoteImpressaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoteImpressaoService {

    private final LoteImpressaoRepository loteRepository;

    @Transactional(readOnly = true)
    public List<LoteImpressaoDTO> listarTodos() {
        return loteRepository.findByAtivoTrueOrderByNumeroLoteAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
