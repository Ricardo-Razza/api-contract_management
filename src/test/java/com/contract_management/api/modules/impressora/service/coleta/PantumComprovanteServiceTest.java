package com.contract_management.api.modules.impressora.service.coleta;

import com.contract_management.api.modules.impressora.model.ColetaContadorItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PantumComprovanteServiceTest {
    @TempDir Path pasta;

    @Test
    void geraPngSemNavegadorComDadosDaColeta() throws Exception {
        ColetaContadorItem item = ColetaContadorItem.builder()
                .modelo("Pantum M7105DW").ip("192.168.8.27").numeroSerie("CP2SV003P4")
                .contadorTotal(8501).nivelToner(23).dataColeta(LocalDateTime.of(2026, 10, 6, 12, 30))
                .build();
        Path arquivo = pasta.resolve("comprovantes/pantum.png");
        new PantumComprovanteService().gerar(item, arquivo);
        var imagem = ImageIO.read(arquivo.toFile());
        assertNotNull(imagem);
        assertEquals(1100, imagem.getWidth());
        assertEquals(820, imagem.getHeight());
        assertNotEquals(imagem.getRGB(0, 0), imagem.getRGB(50, 50));
    }

    @Test
    void naoGeraComprovanteSemLeitura() {
        ColetaContadorItem item = ColetaContadorItem.builder().modelo("Pantum P3305DW").build();
        assertThrows(IllegalArgumentException.class,
                () -> new PantumComprovanteService().gerar(item, pasta.resolve("sem-contador.png")));
    }
}
