package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "instalacao_impressora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstalacaoImpressora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impressora_id", nullable = false)
    private Impressora impressora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secretaria_id", nullable = false)
    private Secretaria secretaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empenho_id")
    private EmpenhoImpressao empenho;

    @Column(name = "local_instalacao", nullable = false)
    private String localInstalacao;

    @Column(name = "endereco")
    private String endereco;

    @Column(name = "responsavel", length = 150)
    private String responsavel;

    @Column(name = "transformador", length = 50)
    private String transformador;

    @Column(name = "data_instalacao", nullable = false)
    private LocalDate dataInstalacao;

    @Column(name = "contador_instalacao_mono", nullable = false)
    @Builder.Default
    private Integer contadorInstalacaoMono = 0;

    @Column(name = "contador_instalacao_color", nullable = false)
    @Builder.Default
    private Integer contadorInstalacaoColor = 0;

    @Column(name = "data_retirada")
    private LocalDate dataRetirada;

    @Column(name = "contador_retirada_mono")
    private Integer contadorRetiradaMono;

    @Column(name = "contador_retirada_color")
    private Integer contadorRetiradaColor;

    @Column(name = "motivo_retirada")
    private String motivoRetirada;

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "ATIVA";
}
