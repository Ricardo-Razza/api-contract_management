package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspelhoFaturaDTO {
    private Long empenhoId;
    private String numeroEmpenho;
    private Integer ano;
    private String secretariaNome;
    private String secretariaSigla;
    private String contratoNumero;
    private Integer mesReferencia;
    private Integer anoReferencia;
    private String competenciaFormatada;
    private LocalDate dataEmissao;
    private BigDecimal totalFatura;
    private List<ItemFaturaDTO> itens;
    private List<EquipamentoFaturaDTO> equipamentos;
    private String textoAtesto;
}
