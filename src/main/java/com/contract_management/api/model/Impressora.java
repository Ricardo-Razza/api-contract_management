package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "impressora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Impressora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_pedido")
    private Integer itemPedido;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Column(name = "fabricante", length = 100, nullable = false)
    private String fabricante;

    @Column(name = "modelo", length = 100, nullable = false)
    private String modelo;

    @Column(name = "tipo_impressao", length = 30, nullable = false)
    @Builder.Default
    private String tipoImpressao = "MONO";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private LoteImpressao lote;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "impressora", cascade = CascadeType.ALL)
    @Builder.Default
    private List<InstalacaoImpressora> instalacoes = new ArrayList<>();
}
