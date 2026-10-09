package com.contract_management.api.modules.ativo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ativo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ativo {

    public static final String SITUACAO_ATIVO = "ATIVO";
    public static final String SITUACAO_DESATIVADO = "DESATIVADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "situacao", length = 255, nullable = false)
    private String situacao;
}