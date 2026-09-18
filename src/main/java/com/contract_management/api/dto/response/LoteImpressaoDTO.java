package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteImpressaoDTO {
    private Long id;
    private Integer numeroLote;
    private String descricao;
    private String tipo;
    private Integer franquiaMono;
    private Integer franquiaColor;
    private BigDecimal valorLocacaoMensal;
    private BigDecimal valorExcedenteMono;
    private BigDecimal valorExcedenteColor;
    private Boolean ativo;
}
