package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipamentoFaturaDTO {
    private Integer itemPedido;
    private String modelo;
    private String numeroSerie;
    private String localInstalacao;
    private Integer numeroLote;
    private Integer leituraMonoAnterior;
    private Integer leituraMonoAtual;
    private Integer copiasMono;
    private Integer leituraColorAnterior;
    private Integer leituraColorAtual;
    private Integer copiasColor;
    private Integer franquiaMono;
    private Integer franquiaColor;
    private Integer excedenteMono;
    private Integer excedenteColor;
    private BigDecimal valorLocacao;
    private BigDecimal valorExcedente;
    private BigDecimal valorTotal;
    private String origemLeitura;
    private String observacoes;
}
