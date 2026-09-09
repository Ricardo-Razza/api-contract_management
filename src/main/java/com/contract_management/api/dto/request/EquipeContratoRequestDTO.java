package com.contract_management.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipeContratoRequestDTO {

    private Long ataId;

    private Long contratoId;

    @NotNull(message = "O status de ativo é obrigatório")
    private Long ativoId;

    @NotEmpty(message = "A equipe deve ter ao menos um membro designado")
    private List<MembroEquipeRequestDTO> membros;
}
