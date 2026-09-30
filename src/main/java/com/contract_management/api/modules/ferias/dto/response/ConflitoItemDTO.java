package com.contract_management.api.modules.ferias.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflitoItemDTO {
    private Long agendamentoId;
    private Long servidorId;
    private String servidorNome;
    private Integer servidorMatricula;
    private String setor;
    private String tipoAfastamento;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Integer dias;
}