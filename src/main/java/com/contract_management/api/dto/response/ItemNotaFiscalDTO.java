package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemNotaFiscalDTO {
    private Integer itemNumero;
    private String codigoItem;
    private String descricao;
    private String unidade;
    private BigDecimal valorUnitario;
    private List<MesFaturaDTO> meses;
}
