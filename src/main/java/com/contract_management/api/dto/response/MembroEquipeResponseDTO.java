package com.contract_management.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembroEquipeResponseDTO {
    private Long id;
    private Long servidorId;
    private String servidorNome;
    private String servidorCargo;
    private String servidorMatricula;
    private Long funcaoId;
    private String funcaoNome;
}
