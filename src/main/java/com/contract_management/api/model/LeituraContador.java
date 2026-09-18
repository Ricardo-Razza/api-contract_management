package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leitura_contador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeituraContador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impressora_id", nullable = false)
    private Impressora impressora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instalacao_id")
    private InstalacaoImpressora instalacao;

    @Column(name = "mes_referencia", nullable = false)
    private Integer mesReferencia;

    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    @Column(name = "data_leitura", nullable = false)
    private LocalDate dataLeitura;

    @Column(name = "leitura_mono_anterior", nullable = false)
    @Builder.Default
    private Integer leituraMonoAnterior = 0;

    @Column(name = "leitura_mono_atual", nullable = false)
    @Builder.Default
    private Integer leituraMonoAtual = 0;

    @Column(name = "copias_mono", nullable = false)
    @Builder.Default
    private Integer copiasMono = 0;

    @Column(name = "leitura_color_anterior", nullable = false)
    @Builder.Default
    private Integer leituraColorAnterior = 0;

    @Column(name = "leitura_color_atual", nullable = false)
    @Builder.Default
    private Integer leituraColorAtual = 0;

    @Column(name = "copias_color", nullable = false)
    @Builder.Default
    private Integer copiasColor = 0;

    @Column(name = "proporcao", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal proporcao = BigDecimal.ONE;

    @Column(name = "franquia_mono_aplicada", nullable = false)
    @Builder.Default
    private Integer franquiaMonoAplicada = 0;

    @Column(name = "franquia_color_aplicada", nullable = false)
    @Builder.Default
    private Integer franquiaColorAplicada = 0;

    @Column(name = "excedente_mono", nullable = false)
    @Builder.Default
    private Integer excedenteMono = 0;

    @Column(name = "excedente_color", nullable = false)
    @Builder.Default
    private Integer excedenteColor = 0;

    @Column(name = "valor_locacao", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorLocacao = BigDecimal.ZERO;

    @Column(name = "valor_excedente_mono", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorExcedenteMono = BigDecimal.ZERO;

    @Column(name = "valor_excedente_color", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorExcedenteColor = BigDecimal.ZERO;

    @Column(name = "valor_total", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "origem_leitura", length = 30, nullable = false)
    @Builder.Default
    private String origemLeitura = "MANUAL";

    @Column(name = "observacoes")
    private String observacoes;
}
