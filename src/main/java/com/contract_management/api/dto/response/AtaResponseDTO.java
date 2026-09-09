package com.contract_management.api.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

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