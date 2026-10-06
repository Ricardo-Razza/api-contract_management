package com.contract_management.api.modules.impressora.service;

import com.contract_management.api.modules.impressora.model.ColetaContadorItem;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PantumComprovanteService {
    public void gerar(ColetaContadorItem item, Path destino) throws IOException {
        if (item.getContadorTotal() == null || item.getContadorTotal() < 0 || item.getDataColeta() == null) {
            throw new IllegalArgumentException("Comprovante requer contador e data de coleta.");
        }
        BufferedImage imagem = new BufferedImage(1100, 820, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(241, 245, 249));
            g.fillRect(0, 0, 1100, 820);
            g.setColor(Color.WHITE);
            g.fillRoundRect(36, 36, 1028, 748, 24, 24);
            g.setColor(new Color(15, 23, 42));
            g.fillRoundRect(36, 36, 1028, 144, 24, 24);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.drawString("PANTUM | Comprovante de contador", 68, 96);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.drawString("Leitura automática por SNMP", 68, 138);
            linha(g, "Modelo", item.getModelo(), 226);
            linha(g, "IP / Número de série", texto(item.getIp()) + " / " + texto(item.getNumeroSerie()), 268);
            linha(g, "Secretaria / Local", texto(item.getSecretariaSigla()) + " / " + texto(item.getLocalInstalacao()), 310);
            linha(g, "Data e hora da coleta", item.getDataColeta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), 352);
            g.setColor(new Color(239, 246, 255));
            g.fillRoundRect(68, 384, 964, 150, 18, 18);
            g.setColor(new Color(30, 64, 175));
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString("HODÔMETRO TOTAL", 96, 423);
            g.setFont(new Font("SansSerif", Font.BOLD, 56));
            g.drawString(NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(item.getContadorTotal()) + " páginas", 96, 495);
            linha(g, "Toner restante", item.getNivelToner() == null ? "Não informado" : item.getNivelToner() + "%", 580);
            linha(g, "Sessão / Item", (item.getSessao() == null ? "—" : item.getSessao().getId()) + " / " + texto(item.getItemPedido()), 622);
            g.setColor(new Color(100, 116, 139));
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.drawString("Documento gerado pelo Contract Management a partir dos dados coletados.", 68, 704);
            g.drawString("Fonte: SNMP da impressora. Este comprovante não é uma captura da interface web.", 68, 738);
        } finally {
            g.dispose();
        }
        Files.createDirectories(destino.toAbsolutePath().getParent());
        if (!ImageIO.write(imagem, "png", destino.toFile())) throw new IOException("Não foi possível gerar o PNG.");
    }

    private void linha(Graphics2D g, String rotulo, Object valor, int y) {
        g.setColor(new Color(100, 116, 139));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString(rotulo, 68, y);
        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        String conteudo = texto(valor);
        while (g.getFontMetrics().stringWidth(conteudo) > 660 && conteudo.length() > 1) {
            conteudo = conteudo.substring(0, conteudo.length() - 1);
        }
        if (!conteudo.equals(texto(valor))) conteudo = conteudo.substring(0, Math.max(0, conteudo.length() - 1)) + "…";
        g.drawString(conteudo, 365, y);
    }

    private String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? "Não informado" : valor.toString();
    }
}
