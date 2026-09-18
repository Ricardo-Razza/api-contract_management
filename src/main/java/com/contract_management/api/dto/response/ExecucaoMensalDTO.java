package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecucaoMensalDTO {
    private Integer ano;
    private BigDecimal totalGeralEmpenhado;
    private BigDecimal totalGeralLiquidado;
    private BigDecimal totalGeralProjetado;
    private BigDecimal saldoGeralRestante;
    private Double percentualGeralConsumido;
    private List<ValorMesDTO> totaisMensais;
    private List<EmpenhoExecucaoDTO> empenhos;
}
