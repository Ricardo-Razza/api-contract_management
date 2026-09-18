package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValorMesDTO {
    private Integer mes;
    private String nomeMes;
    private BigDecimal valorFaturado;
    private Integer copiasMono;
    private Integer copiasColor;
    private String status;
}
