package com.contract_management.api.modules.contrato.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.model.AtaRegistroPreco;
import com.contract_management.api.modules.contrato.model.AtaSecretaria;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.equipe.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.modules.equipe.model.EquipeContrato;
import com.contract_management.api.modules.equipe.model.EquipeMembro;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;

@Mapper(componentModel = "spring")
public interface AtaMapper {

    @Mapping(target = "tipo", source = "tipo.tipoArp")
    @Mapping(target = "situacao", source = "ativo.situacao")
    @Mapping(target = "secretarias", source = "secretarias")
    @Mapping(target = "equipe", source = "equipe")
    AtaResponseDTO toResponseDTO(AtaRegistroPreco ata);

    List<AtaResponseDTO> toResponseDTOList(List<AtaRegistroPreco> atas);

    @Mapping(target = "id", source = "secretaria.id")
    @Mapping(target = "nome", source = "secretaria.nome")
    @Mapping(target = "sigla", source = "secretaria.sigla")
    @Mapping(target = "situacao", source = "ativo.situacao")
    SecretariaResponseDTO toSecretariaResponseDTO(AtaSecretaria as);

    List<SecretariaResponseDTO> toSecretariaResponseDTOList(List<AtaSecretaria> secretarias);

    @Mapping(target = "ataId", source = "ata.id")
    @Mapping(target = "ataNumero", source = "ata.numero")
    @Mapping(target = "ataAno", source = "ata.ano")
    @Mapping(target = "ataObjeto", source = "ata.objeto")
    @Mapping(target = "ativoId", source = "ativo.id")
    @Mapping(target = "situacao", source = "ativo.situacao")
    @Mapping(target = "contratoId", ignore = true)
    @Mapping(target = "contratoNumero", ignore = true)
    @Mapping(target = "contratoAno", ignore = true)
    @Mapping(target = "contratoObjeto", ignore = true)
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
