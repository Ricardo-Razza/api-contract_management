package com.contract_management.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColetaItemDTO {

    private Long id;
    private Long sessaoId;
    private Long impressoraId;
    private Integer itemPedido;
    private String ip;
    private String modelo;
    private String secretariaSigla;
    private String localInstalacao;
    private String status; // SUCESSO, OFFLINE, ERRO, PENDENTE
    private String mensagem;
    private String nomeArquivo;
    private String urlImagem;
    private Integer contadorTotal;
    private Integer contadorMono;
    private Integer contadorColor;
    private Integer copiasPrint;
    private Integer copiasCopiador;
    private Integer copiasScanner;
    private LocalDateTime dataColeta;
}
