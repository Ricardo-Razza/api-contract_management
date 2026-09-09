package com.contract_management.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ContratoRequestDTO {

    @NotNull(message = "Número é obrigatório")
    @Min(value = 1, message = "Número deve ser maior que zero")
    private Integer numero;

    @NotNull(message = "Ano é obrigatório")
    @Min(value = 2000, message = "Ano deve ser maior que 2000")
    private Integer ano;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private LocalDate dataFim;

    @NotNull(message = "Tipo é obrigatório")
    private Long tipoId;

    @NotBlank(message = "Objeto é obrigatório")
    private String objeto;

    @NotBlank(message = "Nome do contratado é obrigatório")
    private String nomeContratado;

    @NotBlank(message = "Portaria de designação é obrigatória")
    private String portariaDesignacao;

    @NotNull(message = "Data de designação é obrigatória")
    private LocalDate dataDesignacao;

    @NotNull(message = "Status é obrigatório")
    private Long ativoId;

    private List<Long> secretariasIds;

    private String observacao;
}
