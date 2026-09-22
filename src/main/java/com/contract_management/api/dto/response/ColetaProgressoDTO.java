package com.contract_management.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColetaProgressoDTO {

    private Long sessaoId;
    private String status; // EM_ANDAMENTO, CONCLUIDO, ERRO, CANCELADO
    private Integer anoReferencia;
    private Integer mesReferencia;
    private Integer total;
    private Integer processadas;
    private Integer sucessos;
    private Integer falhas;
    private Integer percentual;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Boolean emAndamento;
    private String ultimaMensagem;
}
