package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteBalancoDTO {
    private Long loteId;
    private Integer numeroLote;
    private String descricao;
    private String tipo;
    private Integer quantidadeEquipamentos;
    private Integer franquiaIndividualMono;
    private Integer franquiaIndividualColor;
    private Integer franquiaTotalMono;
    private Integer franquiaTotalColor;
    private Integer copiasMonoProduzidas;
    private Integer copiasColorProduzidas;
    private Integer excedenteMonoTotal;
    private Integer excedenteColorTotal;
    private Double percentualUsoMono;
    private Double percentualUsoColor;
    private BigDecimal valorLocacaoUnitario;
    private BigDecimal valorExcedenteMonoUnitario;
    private BigDecimal valorExcedenteColorUnitario;
    private BigDecimal custoFixoLocacao;
    private BigDecimal custoExcedenteMono;
    private BigDecimal custoExcedenteColor;
    private BigDecimal custoTotal;
}
