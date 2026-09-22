package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coleta_contador_sessao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColetaContadorSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    @Column(name = "mes_referencia", nullable = false)
    private Integer mesReferencia;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(name = "status", length = 50, nullable = false)
    private String status; // EM_ANDAMENTO, CONCLUIDO, ERRO, CANCELADO

    @Column(name = "total_impressoras", nullable = false)
    @Builder.Default
    private Integer totalImpressoras = 0;

    @Column(name = "total_sucesso", nullable = false)
    @Builder.Default
    private Integer totalSucesso = 0;

    @Column(name = "total_falhas", nullable = false)
    @Builder.Default
    private Integer totalFalhas = 0;

    @Column(name = "diretorio_prints", length = 255)
    private String diretorioPrints;

    @OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ColetaContadorItem> itens = new ArrayList<>();
}
