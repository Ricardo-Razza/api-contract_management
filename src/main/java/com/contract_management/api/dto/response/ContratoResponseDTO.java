package com.contract_management.api.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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
