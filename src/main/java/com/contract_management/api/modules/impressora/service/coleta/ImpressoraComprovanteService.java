package com.contract_management.api.modules.impressora.service.coleta;

import com.contract_management.api.modules.impressora.model.ColetaContadorItem;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ImpressoraComprovanteService {
    public void gerar(ColetaContadorItem item, Path destino) throws IOException {
        if (item.getContadorTotal() == null || item.getContadorTotal() < 0 || item.getDataColeta() == null) {
            throw new IllegalArgumentException("Comprovante requer contador e data de coleta.");
        }
        BufferedImage imagem = new BufferedImage(1100, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(241, 245, 249));
            g.fillRect(0, 0, 1100, 900);
            g.setColor(Color.WHITE);
            g.fillRoundRect(36, 36, 1028, 828, 24, 24);
            g.setColor(new Color(15, 23, 42));
            g.fillRoundRect(36, 36, 1028, 144, 24, 24);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.drawString("Comprovante de coleta automática", 68, 96);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.drawString("Contract Management | Contadores da impressora", 68, 138);
            linha(g, "Modelo", item.getModelo(), 226);
            linha(g, "IP / Número de série", texto(item.getIp()) + " / " + texto(item.getNumeroSerie()), 268);
            linha(g, "Secretaria / Local", texto(item.getSecretariaSigla()) + " / " + texto(item.getLocalInstalacao()), 310);
            linha(g, "Data e hora da coleta", item.getDataColeta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), 352);
            g.setColor(new Color(239, 246, 255));
            g.fillRoundRect(68, 384, 964, 134, 18, 18);
            g.setColor(new Color(30, 64, 175));
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString("CONTADOR TOTAL", 96, 423);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            g.drawString(contador(item.getContadorTotal()) + " páginas", 96, 487);
            linha(g, "Impressões", contador(item.getCopiasPrint()), 560);
            linha(g, "Cópias", contador(item.getCopiasCopiador()), 604);
            linha(g, "Scanner", contador(item.getCopiasScanner()), 648);
            linha(g, "Toner restante", item.getNivelToner() == null ? null : item.getNivelToner() + "%", 692);
            linha(g, "Fonte da coleta", item.getMetodoColeta(), 736);
            g.setColor(new Color(100, 116, 139));
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.drawString("Documento gerado a partir dos dados retornados pelo equipamento.", 68, 800);
            g.drawString("Não informado: o equipamento não retornou esse dado na coleta.", 68, 830);
        } finally {
            g.dispose();
        }
        Files.createDirectories(destino.toAbsolutePath().getParent());
        if (!ImageIO.write(imagem, "png", destino.toFile())) throw new IOException("Não foi possível gerar o PNG.");
    }

    static String contador(Integer valor) {
        return valor == null || valor < 0 ? "Não informado" :
                NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(valor);
    }

    private void linha(Graphics2D g, String rotulo, Object valor, int y) {
        g.setColor(new Color(100, 116, 139));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString(rotulo, 68, y);
        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        String original = texto(valor);
        String conteudo = original;
        while (g.getFontMetrics().stringWidth(conteudo) > 640 && conteudo.length() > 1) {
            conteudo = conteudo.substring(0, conteudo.length() - 1);
        }
        if (!conteudo.equals(original)) conteudo = conteudo.substring(0, Math.max(0, conteudo.length() - 1)) + "…";
        g.drawString(conteudo, 365, y);
    }

    private String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? "Não informado" : valor.toString();
    }
}
