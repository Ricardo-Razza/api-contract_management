package com.contract_management.api.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ata_registro_preco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtaRegistroPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = true)
    private LocalDate dataFim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_id", nullable = false)
    private Tipo tipo;

    @Column(name = "objeto", columnDefinition = "TEXT", nullable = false)
    private String objeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_id", nullable = false)
    private Ativo ativo;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "ata", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AtaSecretaria> secretarias = new ArrayList<>();

    @OneToMany(mappedBy = "ata", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EquipeContrato> equipe = new ArrayList<>();

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "portaria_designacao", length = 100)
    private String portariaDesignacao;

    @Column(name = "data_designacao", nullable = true)
    private LocalDate dataDesignacao;
}