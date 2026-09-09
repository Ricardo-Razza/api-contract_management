package com.contract_management.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembroEquipeRequestDTO {

    @NotNull(message = "O ID do servidor é obrigatório")
    private Long servidorId;

    @NotNull(message = "O ID da função é obrigatório")
    private Long funcaoId;
}
