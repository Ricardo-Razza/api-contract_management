package com.contract_management.api.modules.ferias.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.contract_management.api.modules.servidor.model.Servidor;

@Entity
@Table(name = "periodo_aquisitivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodoAquisitivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servidor_id", nullable = false)
    private Servidor servidor;

    @Column(name = "ano_inicio", nullable = false)
    private Integer anoInicio;

    @Column(name = "ano_fim", nullable = false)
    private Integer anoFim;

    @Column(name = "identificador", length = 30, nullable = false)
    private String identificador;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "limite_gozo")
    private LocalDate limiteGozo;

    @Builder.Default
    @Column(name = "total_dias", nullable = false)
    private Integer totalDias = 30;

    @Builder.Default
    @Column(name = "dias_usados", nullable = false)
    private Integer diasUsados = 0;

    @Builder.Default
    @Column(name = "dias_restantes", nullable = false)
    private Integer diasRestantes = 30;

    @Builder.Default
    @Column(name = "cor_hex", length = 20, nullable = false)
    private String corHex = "#eab308";

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}