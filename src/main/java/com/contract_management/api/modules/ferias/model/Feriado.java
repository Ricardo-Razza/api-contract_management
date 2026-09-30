package com.contract_management.api.modules.ferias.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "feriado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feriado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false, unique = true)
    private LocalDate data;

    @Column(name = "descricao", length = 100, nullable = false)
    private String descricao;

    @Column(name = "tipo", length = 30, nullable = false)
    private String tipo;
}