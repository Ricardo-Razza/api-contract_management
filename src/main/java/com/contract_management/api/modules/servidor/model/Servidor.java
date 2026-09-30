package com.contract_management.api.modules.servidor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.contract_management.api.common.model.Ativo;
import com.contract_management.api.modules.equipe.model.EquipeMembro;
import com.contract_management.api.modules.secretaria.model.Secretaria;

@Entity
@Table(name = "servidor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", length = 200, nullable = false)
    private String nome;

    @Column(name = "cargo", length = 50, nullable = false)
    private String cargo;

    @Column(name = "setor", length = 100)
    private String setor;

    @Column(name = "matricula", nullable = false)
    private Integer matricula;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "telefone", length = 20, nullable = false)
    private String telefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secretaria_id")
    private Secretaria secretaria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativo_id", nullable = false)
    private Ativo ativo;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Builder.Default
    @OneToMany(mappedBy = "servidor")
    private List<EquipeMembro> equipesMembro = new ArrayList<>();

}