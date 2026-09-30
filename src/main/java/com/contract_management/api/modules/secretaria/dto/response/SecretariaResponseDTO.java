package com.contract_management.api.modules.secretaria.dto.response;

import lombok.Data;

@Data
public class SecretariaResponseDTO {

    private Long id;
    private String nome;
    private String sigla;
    private String situacao;
}