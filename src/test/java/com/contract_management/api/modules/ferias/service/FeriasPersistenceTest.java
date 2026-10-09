package com.contract_management.api.modules.ferias.service;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.ferias.dto.request.AgendamentoFeriasRequestDTO;
import com.contract_management.api.modules.ferias.model.AgendamentoFerias;
import com.contract_management.api.modules.ferias.model.PeriodoAquisitivo;
import com.contract_management.api.modules.ferias.model.StatusFerias;
import com.contract_management.api.modules.ferias.model.TipoAfastamento;
import com.contract_management.api.modules.ferias.repository.AgendamentoFeriasRepository;
import com.contract_management.api.modules.ferias.repository.PeriodoAquisitivoRepository;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.servidor.model.Servidor;

@SpringBootTest(classes=FeriasPersistenceTest.Config.class, webEnvironment=SpringBootTest.WebEnvironment.NONE, properties={
    "spring.datasource.url=jdbc:h2:mem:ferias;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.sql.init.mode=never",
    "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=WARN", "spring.main.banner-mode=off",
    "spring.flyway.enabled=false"
})
class FeriasPersistenceTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.contract_management.api")
    @EnableJpaRepositories(basePackages = "com.contract_management.api")
    @Import({AgendamentoFeriasService.class,PeriodoAquisitivoService.class})
    static class Config {}

    @Autowired EntityManager em;
    @Autowired AgendamentoFeriasRepository agendamentos;
    @Autowired PeriodoAquisitivoRepository periodos;
    @Autowired AgendamentoFeriasService service;
    @Autowired PlatformTransactionManager manager;
    TransactionTemplate tx;
    Servidor ana, bia, outroSetor, outraSecretaria;
    PeriodoAquisitivo periodo;
    @BeforeEach void setup() {
        tx=new TransactionTemplate(manager);
        tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.executeWithoutResult(s -> {
            agendamentos.deleteAll();periodos.deleteAll();
            Ativo ativo=Ativo.builder().situacao("Ativo").build();em.persist(ativo);
            Secretaria sec=Secretaria.builder().nome("Administração").sigla("ADM").ativo(ativo).build();em.persist(sec);
            Secretaria outra=Secretaria.builder().nome("Saúde").sigla("SAU").ativo(ativo).build();em.persist(outra);
            ana=servidor("Ana","TI",sec,ativo);bia=servidor("Bia","TI",sec,ativo);
            outroSetor=servidor("Caio","RH",sec,ativo);outraSecretaria=servidor("Dani","TI",outra,ativo);
            periodo=PeriodoAquisitivo.builder().servidor(ana).anoInicio(2025).anoFim(2026).identificador("2025/2026")
                    .dataInicio(LocalDate.of(2025,1,1)).dataFim(LocalDate.of(2025,12,31)).totalDias(30).diasUsados(0).diasRestantes(30).build();
            em.persist(periodo);em.flush();
        });
    }
    Servidor servidor(String nome,String setor,Secretaria sec,Ativo ativo) {
        var s=Servidor.builder().nome(nome).cargo("Analista").setor(setor).matricula(123).email(nome+"@test.local").telefone("").secretaria(sec).ativo(ativo).build();em.persist(s);return s;
    }
    AgendamentoFerias inserir(Servidor servidor,StatusFerias status) {
        var a=AgendamentoFerias.builder().servidor(servidor).tipoAfastamento(TipoAfastamento.FOLGA).dataInicio(LocalDate.of(2026,10,1))
                .dataFim(LocalDate.of(2026,10,15)).dias(15).status(status).build();em.persist(a);em.flush();return a;
    }
    List<AgendamentoFerias> conflitos(Long excluir) {
        return agendamentos.findConflitos(ana.getId(),LocalDate.of(2026,10,15),LocalDate.of(2026,10,20),ana.getSecretaria().getId(),"TI",excluir);
    }
    AgendamentoFeriasRequestDTO dto(int inicio,int fim) {
        var d=new AgendamentoFeriasRequestDTO();d.setServidorId(ana.getId());d.setPeriodoAquisitivoId(periodo.getId());
        d.setTipoAfastamento(TipoAfastamento.FERIAS);d.setDataInicio(LocalDate.of(2026,10,inicio));d.setDataFim(LocalDate.of(2026,10,fim));d.setStatus(StatusFerias.CONFIRMADO);return d;
    }
    @Test void conflitoApenasProprioServidorOuMesmoSetorDaMesmaSecretaria() {
        tx.executeWithoutResult(s -> {var proprio=inserir(ana,StatusFerias.CONFIRMADO);var colega=inserir(bia,StatusFerias.PLANEJADO);
            inserir(outroSetor,StatusFerias.CONFIRMADO);inserir(outraSecretaria,StatusFerias.CONFIRMADO);
            assertEquals(List.of(proprio.getId(),colega.getId()).stream().sorted().toList(),conflitos(null).stream().map(AgendamentoFerias::getId).sorted().toList());});
    }
    @Test void ignoraCanceladosEOProprioRegistroNaEdicao() {
        tx.executeWithoutResult(s -> {var proprio=inserir(ana,StatusFerias.CONFIRMADO);inserir(bia,StatusFerias.CANCELADO);assertTrue(conflitos(proprio.getId()).isEmpty());});
    }
    @Test void diaLimiteSobrepoeMasDiaSeguinteNao() {
        tx.executeWithoutResult(s -> {inserir(ana,StatusFerias.CONFIRMADO);assertEquals(1,conflitos(null).size());assertTrue(agendamentos.findConflitos(ana.getId(),LocalDate.of(2026,10,16),LocalDate.of(2026,10,20),ana.getSecretaria().getId(),"TI",null).isEmpty());});
    }
    @Test void edicaoSemSaldoFazRollbackDoEstorno() {
        var primeiro=service.criar(dto(1,10));service.criar(dto(21,31));
        assertThrows(BusinessException.class,()->service.atualizar(primeiro.getId(),dto(1,20)));
        tx.executeWithoutResult(s->{em.clear();assertEquals(9,periodos.findById(periodo.getId()).orElseThrow().getDiasRestantes());assertEquals(10,agendamentos.findById(primeiro.getId()).orElseThrow().getDias());});
    }
    @Test void cancelamentoEhIdempotenteNoBanco() {
        var ag=service.criar(dto(1,15));service.cancelar(ag.getId());service.cancelar(ag.getId());
        tx.executeWithoutResult(s->{em.clear();assertEquals(30,periodos.findById(periodo.getId()).orElseThrow().getDiasRestantes());assertEquals(StatusFerias.CANCELADO,agendamentos.findById(ag.getId()).orElseThrow().getStatus());});
    }
    @Test void duasReservasSimultaneasNaoPodemConsumirOMesmoSaldo() throws Exception {
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        var consumed = new java.util.concurrent.atomic.AtomicInteger();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> primeira = () -> {
                ready.countDown(); start.await();
                try { consumed.addAndGet(service.criar(dto(1,20)).getDias()); return true; }
                catch (BusinessException expected) { return false; }
            };
            java.util.concurrent.Callable<Boolean> segunda = () -> {
                ready.countDown(); start.await();
                try { consumed.addAndGet(service.criar(dto(21,31)).getDias()); return true; }
                catch (BusinessException expected) { return false; }
            };
            var a=pool.submit(primeira);var b=pool.submit(segunda);
            assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS));start.countDown();
            assertNotEquals(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS));
        }
        tx.executeWithoutResult(s -> {em.clear();var p=periodos.findById(periodo.getId()).orElseThrow();assertEquals(30-consumed.get(),p.getDiasRestantes());assertEquals(consumed.get(),p.getDiasUsados());});
    }

}