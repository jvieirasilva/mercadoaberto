package com.security.service;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
    // Método para enviar o e-mail
	public static void sendEmail(String toEmail, String newPassword) {
	    // Configurações do servidor SMTP
	    String host = "smtp.gmail.com"; // Substitua pelo servidor SMTP que você está usando
	    final String fromEmail = "rodriguesreginavieira@gmail.com"; // Substitua pelo seu e-mail
	    final String password = "yynw civi ejqb taph"; // Substitua pela sua senha

	    // Configuração das propriedades
	    Properties props = new Properties();
	    props.put("mail.smtp.host", host);
	    props.put("mail.smtp.port", "587"); // Porta padrão para SMTP
	    props.put("mail.smtp.auth", "true");
	    props.put("mail.smtp.starttls.enable", "true"); // Ativar TLS

	    // Autenticação da sessão
	    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
	        protected PasswordAuthentication getPasswordAuthentication() {
	            return new PasswordAuthentication(fromEmail, password);
	        }
	    });

	    try {
	        // Criar a mensagem de e-mail
	        Message message = new MimeMessage(session);
	        message.setFrom(new InternetAddress(fromEmail));
	        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
	        message.setSubject("You have been registered!");

	        // Criar o corpo do e-mail em HTML
	        String body = "<html>"
	                + "<body>"
	                + "<p>You have been registered. Here are your login details:</p>"
	                + "<p><strong>Login:</strong> " + toEmail + "</p>"
	                + "<p><strong>Password:</strong> " + newPassword + "</p>"
	                + "<p><strong>URL:</strong> <a href='http://localhost:8083'>http://localhost:4200</a></p>"
	                + "<p>Please keep this information secure.</p>"
	                + "</body>"
	                + "</html>";

	        // Adicionar o conteúdo HTML
	        message.setContent(body, "text/html; charset=utf-8");

	        // Enviar o e-mail
	        Transport.send(message);

	        System.out.println("Email sent successfully!");
	    } catch (MessagingException e) {
	        e.printStackTrace();
	    }
	}


}