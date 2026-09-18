package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lote_impressao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteImpressao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_lote", nullable = false)
    private Integer numeroLote;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "tipo", length = 50, nullable = false)
    private String tipo;

    @Column(name = "franquia_mono", nullable = false)
    private Integer franquiaMono;

    @Column(name = "franquia_color", nullable = false)
    private Integer franquiaColor;

    @Column(name = "valor_locacao_mensal", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorLocacaoMensal;

    @Column(name = "valor_excedente_mono", precision = 10, scale = 4, nullable = false)
    private BigDecimal valorExcedenteMono;

    @Column(name = "valor_excedente_color", precision = 10, scale = 4, nullable = false)
    private BigDecimal valorExcedenteColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
