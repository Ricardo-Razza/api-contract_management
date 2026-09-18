package com.contract_management.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpressoraResponseDTO {

    private Long id;
    private Integer itemPedido;
    private String numeroSerie;
    private String fabricante;
    private String modelo;
    private String tipoImpressao;
    private String ip;
    private Boolean ativo;

    // Dados do Lote
    private Long loteId;
    private Integer numeroLote;
    private String loteDescricao;
    private Integer franquiaMono;
    private Integer franquiaColor;
    private BigDecimal valorLocacaoMensal;
    private BigDecimal valorExcedenteMono;
    private BigDecimal valorExcedenteColor;

    // Dados da Instalação Ativa
    private Long instalacaoId;
    private Long secretariaId;
    private String secretariaNome;
    private String secretariaSigla;
    private Long empenhoId;
    private String numeroEmpenho;
    private String localInstalacao;
    private String endereco;
    private String responsavel;
    private String transformador;
    private LocalDate dataInstalacao;
    private Integer contadorInstalacaoMono;
    private Integer contadorInstalacaoColor;
    private String statusInstalacao;

    // Última leitura conhecida
    private Integer ultimoContadorMono;
    private Integer ultimoContadorColor;
    private LocalDate dataUltimaLeitura;
}
