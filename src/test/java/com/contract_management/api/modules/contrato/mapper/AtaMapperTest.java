package com.contract_management.api.modules.contrato.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.List;

import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.model.AtaRegistroPreco;
import com.contract_management.api.modules.contrato.model.AtaSecretaria;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.equipe.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.modules.equipe.model.EquipeContrato;
import com.contract_management.api.modules.equipe.model.EquipeMembro;
import com.contract_management.api.modules.equipe.model.FuncaoEquipe;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.servidor.model.Servidor;

import static org.assertj.core.api.Assertions.assertThat;

class AtaMapperTest {

    private final AtaMapper mapper = Mappers.getMapper(AtaMapper.class);

    @Test
    @DisplayName("Deve mapear AtaRegistroPreco completa para AtaResponseDTO")
    void deveMapearAtaCompleta() {
        Tipo tipo = Tipo.builder().id(1L).tipoArp("MATERIAL").build();
        Ativo ativo = Ativo.builder().id(2L).situacao("ATIVO").build();
        Secretaria sec = Secretaria.builder().id(10L).nome("Educação").sigla("SME").build();
        Servidor servidor = Servidor.builder().id(100L).nome("Maria").cargo("Gestor").matricula(67890).build();
        FuncaoEquipe funcao = FuncaoEquipe.builder().id(200L).nome("Fiscal Técnico").build();

        AtaRegistroPreco ata = AtaRegistroPreco.builder()
                .id(1L)
                .numero(20)
                .ano(2026)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .tipo(tipo)
                .ativo(ativo)
                .objeto("Fornecimento de merenda")
                .observacao("Obs ata")
                .build();

        AtaSecretaria as = AtaSecretaria.builder()
                .id(5L)
                .ata(ata)
                .secretaria(sec)
                .ativo(ativo)
                .build();
        ata.setSecretarias(List.of(as));

        EquipeContrato equipe = EquipeContrato.builder()
                .id(60L)
                .ata(ata)
                .ativo(ativo)
                .build();
        EquipeMembro membro = EquipeMembro.builder()
                .id(600L)
                .equipe(equipe)
                .servidor(servidor)
                .funcao(funcao)
                .build();
        equipe.setMembros(List.of(membro));
        ata.setEquipe(List.of(equipe));

        AtaResponseDTO dto = mapper.toResponseDTO(ata);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNumero()).isEqualTo(20);
        assertThat(dto.getAno()).isEqualTo(2026);
        assertThat(dto.getTipo()).isEqualTo("MATERIAL");
        assertThat(dto.getSituacao()).isEqualTo("ATIVO");
        assertThat(dto.getObjeto()).isEqualTo("Fornecimento de merenda");

        assertThat(dto.getSecretarias()).hasSize(1);
        SecretariaResponseDTO secDTO = dto.getSecretarias().get(0);
        assertThat(secDTO.getId()).isEqualTo(10L);
        assertThat(secDTO.getNome()).isEqualTo("Educação");
        assertThat(secDTO.getSigla()).isEqualTo("SME");

        assertThat(dto.getEquipe()).hasSize(1);
        EquipeContratoResponseDTO eqDTO = dto.getEquipe().get(0);
        assertThat(eqDTO.getId()).isEqualTo(60L);
        assertThat(eqDTO.getAtaId()).isEqualTo(1L);
        assertThat(eqDTO.getMembros()).hasSize(1);
        MembroEquipeResponseDTO mDTO = eqDTO.getMembros().get(0);
        assertThat(mDTO.getId()).isEqualTo(600L);
        assertThat(mDTO.getServidorId()).isEqualTo(100L);
        assertThat(mDTO.getServidorMatricula()).isEqualTo("67890");
    }

    @Test
    @DisplayName("Deve tratar null com segurança")
    void deveTratarNullComSeguranca() {
        AtaRegistroPreco ata = AtaRegistroPreco.builder().id(1L).build();
        AtaResponseDTO dto = mapper.toResponseDTO(ata);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTipo()).isNull();
        assertThat(dto.getSituacao()).isNull();
        assertThat(dto.getSecretarias()).isNullOrEmpty();
        assertThat(dto.getEquipe()).isNullOrEmpty();
    }
}
