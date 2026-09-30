package com.contract_management.api.modules.contrato.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;

@Data
public class ContratoResponseDTO {

    private Long id;
    private Integer numero;
    private Integer ano;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String tipo;
    private String objeto;
    private String nomeContratado;
    private String portariaDesignacao;
    private LocalDate dataDesignacao;
    private String situacao;
    private List<SecretariaResponseDTO> secretarias;
    private List<EquipeContratoResponseDTO> equipe;
    private String observacao;
}