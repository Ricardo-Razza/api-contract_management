package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "local_instalacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalInstalacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secretaria_id", nullable = false)
    private Secretaria secretaria;

    @Column(name = "endereco", length = 255)
    private String endereco;

    @Column(name = "responsavel", length = 150)
    private String responsavel;

    @Column(name = "telefone", length = 50)
    private String telefone;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
