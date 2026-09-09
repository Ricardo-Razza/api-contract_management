package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "notificacao_vencimento_enviada",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notificacao_contrato_dias_datafim",
                        columnNames = {"contrato_id", "dias_alerta", "data_fim_referencia"}
                ),
                @UniqueConstraint(
                        name = "uk_notificacao_ata_dias_datafim",
                        columnNames = {"ata_id", "dias_alerta", "data_fim_referencia"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoVencimentoEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", nullable = true)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ata_id", nullable = true)
    private AtaRegistroPreco ata;

    @Column(name = "dias_alerta", nullable = false)
    private Integer diasAlerta;

    @Column(name = "data_fim_referencia", nullable = false)
    private LocalDate dataFimReferencia;

    @CreationTimestamp
    @Column(name = "enviado_em", updatable = false)
    private LocalDateTime enviadoEm;
}