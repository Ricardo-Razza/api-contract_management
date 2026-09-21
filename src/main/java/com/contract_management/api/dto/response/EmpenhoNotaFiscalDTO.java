package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpenhoNotaFiscalDTO {
    private Long empenhoId;
    private String numeroEmpenho;
    private String secretariaSigla;
    private String secretariaNome;
    private String titulo;
    private Integer quantidadeEquipamentos;
    private List<ItemNotaFiscalDTO> itens;
    private List<BigDecimal> totaisMensais;
    private BigDecimal totalAnual;
}
