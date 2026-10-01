package com.contract_management.api.modules.impressora.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.common.model.Ativo;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.impressora.model.*;
import com.contract_management.api.modules.impressora.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.modules.impressora.dto.request.*;

@SpringBootTest(classes=InstalacaoHistoricoTest.Config.class, webEnvironment=SpringBootTest.WebEnvironment.NONE, properties={
    "spring.datasource.url=jdbc:h2:mem:historico;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.sql.init.mode=never",
    "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=WARN", "spring.main.banner-mode=off"
})
@Transactional
class InstalacaoHistoricoTest {
    @Configuration @EnableAutoConfiguration
    @EntityScan(basePackages="com.contract_management.api")
    @EnableJpaRepositories(basePackages="com.contract_management.api")
    @Import(ImpressoraService.class)
    static class Config {}

    @Autowired EntityManager em;
    @Autowired ImpressoraService service;
    @Autowired InstalacaoImpressoraRepository instalacoes;
    Secretaria secretaria, outra;
    LocalInstalacao origem, destino, homonimo;
    Long impressoraId;

    @BeforeEach void setup() {
        var ativo=Ativo.builder().situacao("Ativo").build(); em.persist(ativo);
        secretaria=Secretaria.builder().nome("Administração").sigla("ADM").ativo(ativo).build(); em.persist(secretaria);
        outra=Secretaria.builder().nome("Saúde").sigla("SAU").ativo(ativo).build(); em.persist(outra);
        origem=local("Recepção",secretaria); destino=local("Protocolo",secretaria); homonimo=local("Recepção",outra);
        impressoraId=service.criar(cadastro(origem)).getId(); em.flush();
    }
    LocalInstalacao local(String nome, Secretaria sec) {
        var l=LocalInstalacao.builder().nome(nome).secretaria(sec).ativo(true).build();em.persist(l);return l;
    }
    ImpressoraRequestDTO cadastro(LocalInstalacao l) {
        return ImpressoraRequestDTO.builder().fabricante("Ricoh").modelo("M1").numeroSerie("SERIE-A")
                .tipoImpressao("MONO").secretariaId(l.getSecretaria().getId()).localInstalacaoId(l.getId())
                .localInstalacao("Texto que deve ser ignorado").dataInstalacao(LocalDate.of(2026,1,1)).build();
    }
    TrocaLocalRequestDTO mudanca(LocalInstalacao l) {
        return TrocaLocalRequestDTO.builder().localInstalacaoId(l.getId()).novaSecretariaId(l.getSecretaria().getId())
                .dataMudanca(LocalDate.of(2026,2,1)).contadorAtualMono(120).motivo("Mudança de setor").build();
    }
    @Test void cadastroUsaIdentidadeENomeDoLocalCadastrado() {
        var h=service.historico(impressoraId);
        assertEquals(1,h.size());assertEquals(origem.getId(),h.getFirst().localInstalacaoId());
        assertEquals("Recepção",h.getFirst().localInstalacao());assertEquals("ATIVA",h.getFirst().status());
    }
    @Test void remanejamentoPreservaPassagensEOrdenaMaisRecentePrimeiro() {
        service.remanejarLocal(impressoraId,mudanca(destino));em.flush();em.clear();
        var h=service.historico(impressoraId);assertEquals(2,h.size());
        assertEquals(destino.getId(),h.get(0).localInstalacaoId());assertNull(h.get(0).dataRetirada());
        assertEquals("REMANEJADA",h.get(1).status());assertEquals(LocalDate.of(2026,2,1),h.get(1).dataRetirada());
        assertEquals("Mudança de setor",h.get(1).motivoRetirada());
        assertEquals(1,instalacoes.findHistoricoByLocalId(origem.getId()).size());
        assertEquals(1,instalacoes.findHistoricoByLocalId(destino.getId()).size());
    }
    @Test void locaisHomonimosNaoMisturamHistoricoERenomearNaoPerdeVinculo() {
        service.criar(cadastro(homonimo));origem.setNome("Recepção renovada");em.flush();em.clear();
        var h=instalacoes.findHistoricoByLocalId(origem.getId());assertEquals(1,h.size());
        assertEquals(impressoraId,h.getFirst().getImpressora().getId());
    }
    @Test void edicaoNaoPodeApagarPassagemAnterior() {
        assertThrows(BusinessException.class,()->service.atualizar(impressoraId,cadastro(destino)));
        assertEquals(origem.getId(),service.historico(impressoraId).getFirst().localInstalacaoId());
    }
    @Test void rejeitaLocalInativoOuDeOutraSecretaria() {
        var dto=cadastro(destino);dto.setSecretariaId(outra.getId());
        assertThrows(BusinessException.class,()->service.criar(dto));
        destino.setAtivo(false);assertThrows(BusinessException.class,()->service.criar(cadastro(destino)));
    }
    @Test void rejeitaMudancaAnteriorAInstalacaoOuMesmoLocal() {
        var dto=mudanca(destino);dto.setDataMudanca(LocalDate.of(2025,12,31));
        assertThrows(BusinessException.class,()->service.remanejarLocal(impressoraId,dto));
        assertThrows(BusinessException.class,()->service.remanejarLocal(impressoraId,mudanca(origem)));
    }
    @Test void recolhimentoPreservaHistoricoDeImpressoraInativa() {
        service.excluir(impressoraId);em.flush();em.clear();
        assertEquals("RECOLHIDA",service.historico(impressoraId).getFirst().status());
        assertEquals(1,instalacoes.findHistoricoByLocalId(origem.getId()).size());
    }
    @Test void impressoraInexistenteRetornaErroDeNaoEncontrada() {
        assertThrows(EntityNotFoundException.class,()->service.historico(-1L));
    }
    @Test void substituicaoPreservaHistoricoDoLocalEDaMaquinaRetirada() {
        var dto=SubstituicaoImpressoraRequestDTO.builder().dataSubstituicao(LocalDate.of(2026,2,1))
                .contadorFinalMonoRetirada(120).motivoDefeito("Falha").novoFabricante("Ricoh").novoModelo("M2")
                .novoNumeroSerie("SERIE-B").contadorInicialMonoNova(0).build();
        var nova=service.substituirPorDefeito(impressoraId,dto);em.flush();em.clear();
        assertEquals(2,instalacoes.findHistoricoByLocalId(origem.getId()).size());
        assertEquals("SUBSTITUIDA",service.historico(impressoraId).getFirst().status());
        assertEquals(origem.getId(),service.historico(nova.getId()).getFirst().localInstalacaoId());
    }
}
