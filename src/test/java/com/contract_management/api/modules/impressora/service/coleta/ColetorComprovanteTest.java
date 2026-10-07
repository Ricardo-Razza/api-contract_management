package com.contract_management.api.modules.impressora.service.coleta;

import com.contract_management.api.modules.impressora.service.medicao.LeituraContadorService;

import com.contract_management.api.modules.impressora.model.*;
import com.contract_management.api.modules.impressora.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import javax.imageio.ImageIO;
import java.net.http.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ColetorComprovanteTest {
    @TempDir Path pasta;
    final ColetaContadorSessaoRepository sessoes = mock(ColetaContadorSessaoRepository.class);
    final ColetaContadorItemRepository itens = mock(ColetaContadorItemRepository.class);
    final SnmpColetorService snmp = mock(SnmpColetorService.class);

    ColetorImpressoraService coletor() {
        return spy(new ColetorImpressoraService(new PantumComprovanteService(), new ImpressoraComprovanteService(),
                mock(ControleIntervaloColetaService.class), sessoes, itens, mock(ImpressoraRepository.class),
                mock(InstalacaoImpressoraRepository.class), mock(LeituraContadorRepository.class),
                mock(LeituraContadorService.class), mock(JdbcTemplate.class), snmp));
    }

    ColetaContadorItem item(String modelo) {
        var sessao = ColetaContadorSessao.builder().id(1L).anoReferencia(2026).mesReferencia(10)
                .diretorioPrints(pasta.resolve("prints").toString()).build();
        return ColetaContadorItem.builder().sessao(sessao).modelo(modelo).ip("192.0.2.1")
                .nomeArquivo("contador.png").caminhoArquivo(pasta.resolve("prints/contador.png").toString()).build();
    }

    @Test void hpESamsungBuscamDetalhesWebMesmoComTotalSnmpESemChrome() throws Exception {
        for (String modelo : List.of("HP Laser MFP 432", "Samsung M4070")) {
            var service = coletor();
            var item = item(modelo);
            doReturn(true).when(service).isPortaAberta(anyString(), anyInt(), anyInt());
            when(snmp.coletar(anyString(), anyString())).thenReturn(new SnmpColetorService.SnmpResultado(
                    true, 100, 100, 0, null, null, null, "SERIE", null, modelo));
            HttpClient http = mock(HttpClient.class);
            HttpResponse<String> resposta = mock(HttpResponse.class);
            when(resposta.statusCode()).thenReturn(200);
            when(resposta.body()).thenReturn("{\"GXI_BILLING_TOTAL_IMP_CNT\":100,"
                    + "\"GXI_BILLING_COPY_TOTAL_IMP_CNT\":0,\"GXI_BILLING_SEND_TO_TOTAL_CNT\":23}");
            when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                    .thenReturn(resposta);
            ReflectionTestUtils.setField(service, "httpClient", http);
            service.executarColetaItem(item, null);
            assertEquals(100, item.getContadorTotal());
            assertEquals(0, item.getCopiasCopiador());
            assertEquals(23, item.getCopiasScanner());
            assertNull(item.getCopiasPrint());
            assertEquals("HIBRIDO", item.getMetodoColeta());
            assertEquals("SUCESSO", item.getStatus());
            assertNotNull(item.getDataColeta());
            assertEquals(900, ImageIO.read(Path.of(item.getCaminhoArquivo()).toFile()).getHeight());
        }
    }

    @Test void recoletaSnmpSemWebLimpaDetalhesAntigosEGeraPng() throws Exception {
        var service = coletor();
        var item = item("Samsung M4070");
        item.setCopiasScanner(900);
        item.setCopiasCopiador(500);
        doReturn(false).when(service).isPortaAberta(anyString(), anyInt(), anyInt());
        when(snmp.coletar(anyString(), anyString())).thenReturn(new SnmpColetorService.SnmpResultado(
                true, 0, 0, 0, null, null, null, "SERIE", null, "Samsung"));
        service.executarColetaItem(item, null);
        assertEquals("SUCESSO", item.getStatus());
        assertEquals("SNMP", item.getMetodoColeta());
        assertNull(item.getCopiasScanner());
        assertNull(item.getCopiasCopiador());
        assertNotNull(ImageIO.read(Path.of(item.getCaminhoArquivo()).toFile()));
    }

    @Test void visualizacaoEZipGeramComprovanteDeColetaSalva() throws Exception {
        var service = coletor();
        var item = item("HP LaserJet");
        item.setContadorTotal(123);
        item.setDataColeta(LocalDateTime.now());
        item.setStatus("SUCESSO");
        when(sessoes.findById(1L)).thenReturn(Optional.of(item.getSessao()));
        when(itens.findBySessaoIdOrderByItemPedidoAsc(1L)).thenReturn(List.of(item));
        when(itens.findBySessaoIdAndStatus(1L, "SUCESSO")).thenReturn(List.of(item));
        assertTrue(service.obterImagem(1L, "contador.png").length > 0);
        Files.delete(Path.of(item.getCaminhoArquivo()));
        try (var zip = new ZipFile(service.gerarZipSessao(1L))) {
            var entrada = zip.getEntry("contador.png");
            assertNotNull(entrada);
            assertEquals(900, ImageIO.read(zip.getInputStream(entrada)).getHeight());
        }
    }
}
