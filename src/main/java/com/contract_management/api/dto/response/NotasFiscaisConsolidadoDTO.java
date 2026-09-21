package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotasFiscaisConsolidadoDTO {
    private Integer ano;
    private String competencia;
    private List<EmpenhoNotaFiscalDTO> empenhos;
    private List<BigDecimal> totaisPrefeituraMensais;
    private BigDecimal totalPrefeituraAnual;
}
