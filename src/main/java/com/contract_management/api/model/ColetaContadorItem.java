package com.contract_management.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coleta_contador_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColetaContadorItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private ColetaContadorSessao sessao;

    @Column(name = "impressora_id")
    private Long impressoraId;

    @Column(name = "item_pedido")
    private Integer itemPedido;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "modelo", length = 100)
    private String modelo;

    @Column(name = "secretaria_sigla", length = 50)
    private String secretariaSigla;

    @Column(name = "local_instalacao", length = 255)
    private String localInstalacao;

    @Column(name = "status", length = 50, nullable = false)
    private String status; // SUCESSO, OFFLINE, ERRO, PENDENTE

    @Column(name = "mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "nome_arquivo", length = 150)
    private String nomeArquivo;

    @Column(name = "caminho_arquivo", length = 255)
    private String caminhoArquivo;

    @Column(name = "contador_total")
    private Integer contadorTotal;

    @Column(name = "contador_mono")
    private Integer contadorMono;

    @Column(name = "contador_color")
    private Integer contadorColor;

    @Column(name = "copias_print")
    private Integer copiasPrint;

    @Column(name = "copias_copiador")
    private Integer copiasCopiador;

    @Column(name = "copias_scanner")
    private Integer copiasScanner;

    @Column(name = "data_coleta")
    private LocalDateTime dataColeta;
}
