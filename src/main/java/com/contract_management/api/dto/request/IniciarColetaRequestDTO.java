package com.contract_management.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IniciarColetaRequestDTO {

    @NotNull(message = "O ano de referência é obrigatório")
    @Min(value = 2020, message = "Ano inválido")
    @Max(value = 2035, message = "Ano inválido")
    private Integer ano;

    @NotNull(message = "O mês de referência é obrigatório")
    @Min(value = 1, message = "O mês deve ser entre 1 e 12")
    @Max(value = 12, message = "O mês deve ser entre 1 e 12")
    private Integer mes;

    private Long empenhoId;

    private Long secretariaId;
}
