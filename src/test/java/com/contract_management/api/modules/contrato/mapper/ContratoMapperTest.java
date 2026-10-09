package com.contract_management.api.modules.contrato.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.List;

import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.contrato.model.Contrato;
import com.contract_management.api.modules.contrato.model.ContratoSecretaria;
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

class ContratoMapperTest {

    private final ContratoMapper mapper = Mappers.getMapper(ContratoMapper.class);

    @Test
    @DisplayName("Deve mapear Contrato completo para ContratoResponseDTO com relacionamentos")
    void deveMapearContratoCompleto() {
        Tipo tipo = Tipo.builder().id(1L).tipoArp("SERVICO").build();
        Ativo ativo = Ativo.builder().id(2L).situacao("ATIVO").build();
        Secretaria sec = Secretaria.builder().id(10L).nome("Saúde").sigla("SMS").build();
        Servidor servidor = Servidor.builder().id(100L).nome("João").cargo("Analista").matricula(12345).build();
        FuncaoEquipe funcao = FuncaoEquipe.builder().id(200L).nome("Fiscal").build();

        Contrato contrato = Contrato.builder()
                .id(1L)
                .numero(10)
                .ano(2026)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .tipo(tipo)
                .ativo(ativo)
                .objeto("Prestação de serviço")
                .nomeContratado("Empresa X")
                .portariaDesignacao("Portaria 123")
                .dataDesignacao(LocalDate.of(2026, 1, 5))
                .observacao("Obs teste")
                .build();

        ContratoSecretaria cs = ContratoSecretaria.builder()
                .id(5L)
                .contrato(contrato)
                .secretaria(sec)
                .ativo(ativo)
                .build();
        contrato.setSecretarias(List.of(cs));

        EquipeContrato equipe = EquipeContrato.builder()
                .id(50L)
                .contrato(contrato)
                .ativo(ativo)
                .build();
        EquipeMembro membro = EquipeMembro.builder()
                .id(500L)
                .equipe(equipe)
                .servidor(servidor)
                .funcao(funcao)
                .build();
        equipe.setMembros(List.of(membro));
        contrato.setEquipe(List.of(equipe));

        ContratoResponseDTO dto = mapper.toResponseDTO(contrato);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNumero()).isEqualTo(10);
        assertThat(dto.getAno()).isEqualTo(2026);
        assertThat(dto.getTipo()).isEqualTo("SERVICO");
        assertThat(dto.getSituacao()).isEqualTo("ATIVO");
        assertThat(dto.getObjeto()).isEqualTo("Prestação de serviço");
        assertThat(dto.getNomeContratado()).isEqualTo("Empresa X");

        assertThat(dto.getSecretarias()).hasSize(1);
        SecretariaResponseDTO secDTO = dto.getSecretarias().get(0);
        assertThat(secDTO.getId()).isEqualTo(10L);
        assertThat(secDTO.getNome()).isEqualTo("Saúde");
        assertThat(secDTO.getSigla()).isEqualTo("SMS");
        assertThat(secDTO.getSituacao()).isEqualTo("ATIVO");

        assertThat(dto.getEquipe()).hasSize(1);
        EquipeContratoResponseDTO eqDTO = dto.getEquipe().get(0);
        assertThat(eqDTO.getId()).isEqualTo(50L);
        assertThat(eqDTO.getContratoId()).isEqualTo(1L);
        assertThat(eqDTO.getSituacao()).isEqualTo("ATIVO");
        assertThat(eqDTO.getMembros()).hasSize(1);
        MembroEquipeResponseDTO mDTO = eqDTO.getMembros().get(0);
        assertThat(mDTO.getId()).isEqualTo(500L);
        assertThat(mDTO.getServidorId()).isEqualTo(100L);
        assertThat(mDTO.getServidorNome()).isEqualTo("João");
        assertThat(mDTO.getServidorCargo()).isEqualTo("Analista");
        assertThat(mDTO.getServidorMatricula()).isEqualTo("12345");
        assertThat(mDTO.getFuncaoId()).isEqualTo(200L);
        assertThat(mDTO.getFuncaoNome()).isEqualTo("Fiscal");
    }

    @Test
    @DisplayName("Deve tratar null com segurança")
    void deveTratarNullComSeguranca() {
        Contrato contrato = Contrato.builder().id(1L).build();
        ContratoResponseDTO dto = mapper.toResponseDTO(contrato);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTipo()).isNull();
        assertThat(dto.getSituacao()).isNull();
        assertThat(dto.getSecretarias()).isNullOrEmpty();
        assertThat(dto.getEquipe()).isNullOrEmpty();
    }
}
