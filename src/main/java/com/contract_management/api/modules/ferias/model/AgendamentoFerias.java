package com.contract_management.api.modules.ferias.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.contract_management.api.modules.servidor.model.Servidor;

@Entity
@Table(name = "agendamento_ferias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoFerias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servidor_id", nullable = false)
    private Servidor servidor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_aquisitivo_id")
    private PeriodoAquisitivo periodoAquisitivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_afastamento", length = 30, nullable = false)
    private TipoAfastamento tipoAfastamento;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "dias", nullable = false)
    private Integer dias;

    @Column(name = "fracao")
    private Integer fracao;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private StatusFerias status = StatusFerias.CONFIRMADO;

    @Builder.Default
    @Column(name = "alerta_conflito", nullable = false)
    private Boolean alertaConflito = false;

    @Column(name = "descricao_conflito", columnDefinition = "TEXT")
    private String descricaoConflito;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}