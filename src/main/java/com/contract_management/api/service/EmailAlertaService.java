package com.contract_management.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAlertaService {

    private final JavaMailSender mailSender;

    public void enviarAlertaVencimento(String destinatario, String nomeServidor,
                                       Integer numeroContrato, Integer anoContrato,
                                       long diasRestantes) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Contrato " + numeroContrato + "/" + anoContrato +
                    " vence em " + diasRestantes + " dias");

            String corpo = String.format("""
                    Olá, %s.

                    O contrato nº %d/%d está com vencimento previsto em %d dias.

                    Por favor, verifique se é necessário providenciar renovação,
                    aditivo ou encerramento.

                    Este é um e-mail automático do sistema de gestão de contratos.
                    """, nomeServidor, numeroContrato, anoContrato, diasRestantes);

            helper.setText(corpo);
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail de alerta", e);
        }
    }

    public void enviarAlertaVencimentoAta(String destinatario, String nomeServidor,
                                          Integer numeroAta, Integer anoAta,
                                          long diasRestantes) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Ata " + numeroAta + "/" + anoAta +
                    " vence em " + diasRestantes + " dias");

            String corpo = String.format("""
                    Olá, %s.

                    A ata de registro de preço nº %d/%d está com vencimento previsto em %d dias.

                    Por favor, verifique se é necessário providenciar renovação,
                    nova licitação ou encerramento.

                    Este é um e-mail automático do sistema de gestão de contratos.
                    """, nomeServidor, numeroAta, anoAta, diasRestantes);

            helper.setText(corpo);
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail de alerta de ata", e);
        }
    }
}