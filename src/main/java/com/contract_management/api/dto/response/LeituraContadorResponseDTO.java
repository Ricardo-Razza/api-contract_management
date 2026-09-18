package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeituraContadorResponseDTO {

    private Long id;
    private Long impressoraId;
    private Integer itemPedido;
    private String impressoraModelo;
    private String impressoraIp;
    private String secretariaSigla;
    private String localInstalacao;

    private Integer mesReferencia;
    private Integer anoReferencia;
    private LocalDate dataLeitura;

    private Integer leituraMonoAnterior;
    private Integer leituraMonoAtual;
    private Integer copiasMono;

    private Integer leituraColorAnterior;
    private Integer leituraColorAtual;
    private Integer copiasColor;

    private BigDecimal proporcao;
    private Integer franquiaMonoAplicada;
    private Integer franquiaColorAplicada;
    private Integer excedenteMono;
    private Integer excedenteColor;

    private BigDecimal valorLocacao;
    private BigDecimal valorExcedenteMono;
    private BigDecimal valorExcedenteColor;
    private BigDecimal valorTotal;

    private String origemLeitura;
    private String observacoes;
}
