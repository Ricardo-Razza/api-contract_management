package com.contract_management.api.modules.ferias.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

import com.contract_management.api.modules.servidor.model.Servidor;

@Data
public class PeriodoAquisitivoRequestDTO {

    @NotNull(message = "ID do Servidor é obrigatório")
    private Long servidorId;

    @NotNull(message = "Ano de início é obrigatório")
    private Integer anoInicio;

    @NotNull(message = "Ano de fim é obrigatório")
    private Integer anoFim;

    private String identificador; // Se nulo, será gerado como anoInicio/anoFim

    @NotNull(message = "Data de início do período é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim do período é obrigatória")
    private LocalDate dataFim;

    private LocalDate limiteGozo;

    private Integer totalDias = 30;

    private String corHex;
}