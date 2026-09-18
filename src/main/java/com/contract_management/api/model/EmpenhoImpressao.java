package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "empenho_impressao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpenhoImpressao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_empenho", length = 50, nullable = false)
    private String numeroEmpenho;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secretaria_id", nullable = false)
    private Secretaria secretaria;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "valor_total", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "saldo", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
