package com.contract_management.api.modules.contrato.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoResponseDTO {
    private Long id;
    private String tipoArp;
}
