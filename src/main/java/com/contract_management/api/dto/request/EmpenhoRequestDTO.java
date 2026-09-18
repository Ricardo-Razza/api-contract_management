package com.contract_management.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpenhoRequestDTO {

    @NotBlank(message = "O número do empenho é obrigatório")
    private String numeroEmpenho;

    @NotNull(message = "O ano do exercício é obrigatório")
    private Integer ano;

    @NotNull(message = "A secretaria é obrigatória")
    private Long secretariaId;

    private String descricao;

    private BigDecimal valorTotal;

    private BigDecimal saldo;
}
