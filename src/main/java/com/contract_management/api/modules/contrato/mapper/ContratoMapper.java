package com.contract_management.api.modules.contrato.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.contrato.model.Contrato;
import com.contract_management.api.modules.contrato.model.ContratoSecretaria;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.equipe.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.modules.equipe.model.EquipeContrato;
import com.contract_management.api.modules.equipe.model.EquipeMembro;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;

@Mapper(componentModel = "spring")
public interface ContratoMapper {

    @Mapping(target = "tipo", source = "tipo.tipoArp")
    @Mapping(target = "situacao", source = "ativo.situacao")
    @Mapping(target = "secretarias", source = "secretarias")
    @Mapping(target = "equipe", source = "equipe")
    ContratoResponseDTO toResponseDTO(Contrato contrato);

    List<ContratoResponseDTO> toResponseDTOList(List<Contrato> contratos);

    @Mapping(target = "id", source = "secretaria.id")
    @Mapping(target = "nome", source = "secretaria.nome")
    @Mapping(target = "sigla", source = "secretaria.sigla")
    @Mapping(target = "situacao", source = "ativo.situacao")
    SecretariaResponseDTO toSecretariaResponseDTO(ContratoSecretaria cs);

    List<SecretariaResponseDTO> toSecretariaResponseDTOList(List<ContratoSecretaria> secretarias);

    @Mapping(target = "contratoId", source = "contrato.id")
    @Mapping(target = "contratoNumero", source = "contrato.numero")
    @Mapping(target = "contratoAno", source = "contrato.ano")
    @Mapping(target = "contratoObjeto", source = "contrato.objeto")
    @Mapping(target = "ativoId", source = "ativo.id")
    @Mapping(target = "situacao", source = "ativo.situacao")
    @Mapping(target = "ataId", ignore = true)
    @Mapping(target = "ataNumero", ignore = true)
    @Mapping(target = "ataAno", ignore = true)
    @Mapping(target = "ataObjeto", ignore = true)
    @Mapping(target = "membros", source = "membros")
    EquipeContratoResponseDTO toEquipeResponseDTO(EquipeContrato equipe);

    List<EquipeContratoResponseDTO> toEquipeResponseDTOList(List<EquipeContrato> equipes);

    @Mapping(target = "servidorId", source = "servidor.id")
    @Mapping(target = "servidorNome", source = "servidor.nome")
    @Mapping(target = "servidorCargo", source = "servidor.cargo")
    @Mapping(target = "servidorMatricula", source = "servidor.matricula")
    @Mapping(target = "funcaoId", source = "funcao.id")
    @Mapping(target = "funcaoNome", source = "funcao.nome")
    MembroEquipeResponseDTO toMembroResponseDTO(EquipeMembro membro);

    List<MembroEquipeResponseDTO> toMembroResponseDTOList(List<EquipeMembro> membros);
}
