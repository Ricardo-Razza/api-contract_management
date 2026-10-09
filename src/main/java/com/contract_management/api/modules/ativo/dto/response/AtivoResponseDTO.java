package com.contract_management.api.modules.ativo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtivoResponseDTO {
    private Long id;
    private String situacao;
}
