package com.contract_management.api.modules.impressora.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemGradeLeituraDTO {

    private Long impressoraId;
    private Integer itemPedido;
    private String fabricante;
    private String modelo;
    private String tipoImpressao; // MONO ou COLOR
    private String ip;
    private Long secretariaId;
    private String secretariaSigla;
    private String secretariaNome;
    private String localInstalacao;
    private Integer numeroLote;
    private Integer franquiaMono;
    private Integer franquiaColor;

    // Dados da medição da competência
    private Long leituraId;
    private LocalDate dataLeitura;
    private Integer leituraMonoAnterior;
    private Integer leituraMonoAtual;
    private Integer copiasMono;
    private Integer leituraColorAnterior;
    private Integer leituraColorAtual;
    private Integer copiasColor;
    private Integer excedenteMono;
    private Integer excedenteColor;
    private BigDecimal valorLocacao;
    private BigDecimal valorTotal;
    private String origemLeitura;
    private String status; // SALVO ou PENDENTE
    private String observacoes;
}
