package com.contract_management.api.modules.contrato.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;

@Data
public class AtaResponseDTO {

    private Long id;
    private Integer numero;
    private Integer ano;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String tipo;
    private String objeto;
    private String observacao;
    private String situacao;
    private LocalDate dataDesignacao;
    private String portariaDesignacao;

    // relacionamentos
    private List<SecretariaResponseDTO> secretarias;
    private List<EquipeContratoResponseDTO> equipe;
}