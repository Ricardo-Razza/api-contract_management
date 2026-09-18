package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalancoFranquiasDTO {
    private Integer mesReferencia;
    private Integer anoReferencia;
    private String competenciaFormatada;
    private Integer totalGeralEquipamentos;
    private Integer totalGeralCopiasMono;
    private Integer totalGeralCopiasColor;
    private Integer totalGeralExcedenteMono;
    private Integer totalGeralExcedenteColor;
    private BigDecimal custoTotalLocacao;
    private BigDecimal custoTotalExcedentes;
    private BigDecimal custoTotalGeral;
    private List<LoteBalancoDTO> lotes;
}
