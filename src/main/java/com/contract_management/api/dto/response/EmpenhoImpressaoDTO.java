package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpenhoImpressaoDTO {
    private Long id;
    private String numeroEmpenho;
    private Integer ano;
    private Long secretariaId;
    private String secretariaNome;
    private String secretariaSigla;
    private String descricao;
    private BigDecimal valorTotal;
    private BigDecimal saldo;
    private Boolean ativo;
}
