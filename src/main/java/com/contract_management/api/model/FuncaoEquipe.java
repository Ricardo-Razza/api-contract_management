package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "funcao_equipe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuncaoEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Builder.Default
    @OneToMany(mappedBy = "funcao")
    private List<EquipeMembro> membros = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "ativo_id")
    private Ativo ativo;

}
