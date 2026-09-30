package com.contract_management.api.modules.ferias.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.contract_management.api.modules.ferias.model.StatusFerias;
import com.contract_management.api.modules.ferias.model.TipoAfastamento;

@Data
public class AgendamentoFeriasResponseDTO {

    private Long id;
    private Long servidorId;
    private String servidorNome;
    private Integer servidorMatricula;
    private String servidorCargo;
    private String servidorSetor;
    private Long secretariaId;
    private String secretariaNome;
    private String secretariaSigla;

    private Long periodoAquisitivoId;
    private String periodoIdentificador;
    private String corHex;

    private TipoAfastamento tipoAfastamento;
    private String tipoDescricao;

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Integer dias;
    private Integer fracao;

    private StatusFerias status;
    private Boolean alertaConflito;
    private String descricaoConflito;
    private String observacao;

    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}