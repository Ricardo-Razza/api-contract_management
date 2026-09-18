package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpenhoExecucaoDTO {
    private Long empenhoId;
    private String numeroEmpenho;
    private String secretariaSigla;
    private String secretariaNome;
    private String descricao;
    private BigDecimal valorTotalEmpenhado;
    private Long quantidadeImpressoras;
    private List<ValorMesDTO> meses;
    private BigDecimal totalLiquidado;
    private BigDecimal totalProjetado;
    private BigDecimal saldoRestante;
    private Double percentualConsumido;
}
