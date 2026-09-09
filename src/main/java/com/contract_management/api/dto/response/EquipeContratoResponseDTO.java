package com.contract_management.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipeContratoResponseDTO {
    private Long id;
    private Long ataId;
    private Integer ataNumero;
    private Integer ataAno;
    private String ataObjeto;
    private Long contratoId;
    private Integer contratoNumero;
    private Integer contratoAno;
    private String contratoObjeto;
    private Long ativoId;
    private String situacao;
    private List<MembroEquipeResponseDTO> membros;
}
