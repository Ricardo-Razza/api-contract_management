package com.contract_management.api.modules.ferias.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.ferias.model.StatusFerias;
import com.contract_management.api.modules.ferias.model.TipoAfastamento;
import com.contract_management.api.modules.servidor.model.Servidor;

@Data
public class AgendamentoFeriasRequestDTO {

    @NotNull(message = "ID do Servidor é obrigatório")
    private Long servidorId;

    private Long periodoAquisitivoId;

    @NotNull(message = "Tipo de afastamento é obrigatório")
    private TipoAfastamento tipoAfastamento = TipoAfastamento.FERIAS;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private LocalDate dataFim;

    private Integer fracao; // 1, 2 ou 3

    private StatusFerias status = StatusFerias.CONFIRMADO;

    private String observacao;

    // Flag para permitir salvar mesmo quando houver alerta de conflito de escala detectado
    private Boolean confirmarComConflito = false;
}