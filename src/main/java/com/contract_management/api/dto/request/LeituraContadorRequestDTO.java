package com.contract_management.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeituraContadorRequestDTO {

    @NotNull(message = "A impressora é obrigatória")
    private Long impressoraId;

    @NotNull(message = "O mês de referência é obrigatório")
    @Min(1) @Max(12)
    private Integer mesReferencia;

    @NotNull(message = "O ano de referência é obrigatório")
    private Integer anoReferencia;

    @NotNull(message = "A data da leitura é obrigatória")
    private LocalDate dataLeitura;

    @NotNull(message = "A leitura mono atual é obrigatória")
    private Integer leituraMonoAtual;

    private Integer leituraColorAtual;

    private BigDecimal proporcao; // pro-rata, padrão 1.0
    private String origemLeitura; // MANUAL ou SNMP
    private String observacoes;
}
