package com.contract_management.api.modules.ferias.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

import com.contract_management.api.modules.servidor.model.Servidor;

@Data
public class VerificacaoConflitoRequestDTO {

    @NotNull(message = "ID do Servidor é obrigatório")
    private Long servidorId;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private LocalDate dataFim;

    private Long agendamentoId; // ID se for edição, para não acusar conflito consigo mesmo
}