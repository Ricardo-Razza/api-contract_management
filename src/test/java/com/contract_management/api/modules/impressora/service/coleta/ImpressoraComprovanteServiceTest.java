package com.contract_management.api.modules.impressora.service.coleta;

import com.contract_management.api.modules.impressora.model.ColetaContadorItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ImpressoraComprovanteServiceTest {
    @TempDir Path pasta;

    @Test void geraPngComContadorZeroEDetalhesOpcionais() throws Exception {
        var item = ColetaContadorItem.builder().modelo("HP Laser MFP 432")
                .contadorTotal(0).copiasCopiador(0).copiasScanner(null)
                .dataColeta(LocalDateTime.now()).metodoColeta("WEB").build();
        var destino = pasta.resolve("hp.png");
        new ImpressoraComprovanteService().gerar(item, destino);
        var imagem = ImageIO.read(destino.toFile());
        assertNotNull(imagem);
        assertEquals(1100, imagem.getWidth());
        assertEquals(900, imagem.getHeight());
        assertEquals("0", ImpressoraComprovanteService.contador(0));
        assertEquals("Não informado", ImpressoraComprovanteService.contador(null));
        assertEquals("12.345", ImpressoraComprovanteService.contador(12345));
    }

    @Test void naoGeraComprovanteSemContadorReal() {
        assertThrows(IllegalArgumentException.class, () -> new ImpressoraComprovanteService()
                .gerar(ColetaContadorItem.builder().dataColeta(LocalDateTime.now()).build(), pasta.resolve("invalido.png")));
    }
}
