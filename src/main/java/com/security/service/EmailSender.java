package com.security.service;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

@Service
public class EmailSender {
    
    private final String username = "rodriguesreginavieira@gmail.com";
    private final String password = "yynw civi ejqb taph";
    
    
    @Value("${app.frontend-url}")
    private String frontendUrl;
    
    
    
    /**
     * Envia email de confirmação de registro
     */
    public void sendConfirmationEmail(String toEmail, String userName, String confirmationToken) {
        String subject = "🎉 Confirme seu cadastro - Order App";
        String body = buildConfirmationEmailBody(userName, confirmationToken);
        sendHtmlEmail(toEmail, subject, body);
    }
    
    /**
     * Envia email notificando que a conta foi confirmada com sucesso
     */
    public void sendEmailConfirmedNotification(String toEmail, String userName) {
        String subject = "✅ Conta Ativada com Sucesso - Order App";
        String body = buildEmailConfirmedBody(userName);
        sendHtmlEmail(toEmail, subject, body);
    }
    /**
     * Envia email com link para resetar senha
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        String subject = "🔑 Reset Your Password - Order App";
        String body = buildPasswordResetEmailBody(userName, resetToken);
        sendHtmlEmail(toEmail, subject, body);
    }
    
    /**
     * Template HTML para email de reset de senha
     */
    private String buildPasswordResetEmailBody(String userName, String token) {
        //String resetUrl = "http://localhost:3000/reset-password?token=" + token;
        String resetUrl = frontendUrl +"/reset-password?token=" + token;
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: ##333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, ##f59e0b 0%%, ##d97706 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: ##f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 15px 40px; background: ##f59e0b; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }
                    .button:hover { background: ##d97706; }
                    .footer { text-align: center; margin-top: 30px; color: ##999; font-size: 12px; }
                    .warning { background: ##fee2e2; border-left: 4px solid ##ef4444; padding: 15px; margin: 20px 0; border-radius: 4px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔑 Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Hello, %s!</h2>
                        <p>We received a request to reset your password for your <strong>Order App</strong> account.</p>
                        <p>Click the button below to reset your password:</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">🔐 Reset Password</a>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Important:</strong>
                            <ul>
                                <li>This link is valid for <strong>1 hour</strong></li>
                                <li>If you didn't request this, please ignore this email</li>
                                <li>Your password won't change until you create a new one</li>
                            </ul>
                        </div>
                        
                        <p><strong>Alternative link:</strong></p>
                        <p style="word-break: break-all; font-size: 12px; color: ##666;">%s</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Order App. All rights reserved.</p>
                        <p>This is an automated email, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl, resetUrl);
    }
    
    /**
     * Template HTML para email de confirmação
     */
    private String buildConfirmationEmailBody(String userName, String token) {
        String confirmationUrl = frontendUrl +"/confirm-register?token=" + token;
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: ##333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, ##667eea 0%%, ##764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: ##f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 15px 40px; background: ##667eea; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }
                    .button:hover { background: ##5568d3; }
                    .footer { text-align: center; margin-top: 30px; color: ##999; font-size: 12px; }
                    .warning { background: ##fff3cd; border-left: 4px solid ##ffc107; padding: 15px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Bem-vindo ao Order App!</h1>
                    </div>
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        <p>Obrigado por se registrar no <strong>Order App</strong>.</p>
                        <p>Para ativar sua conta e começar a usar nossos serviços, clique no botão abaixo:</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">✅ Confirmar Email</a>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Importante:</strong>
                            <ul>
                                <li>Este link é válido por <strong>24 horas</strong></li>
                                <li>Após a confirmação, você poderá fazer login normalmente</li>
                                <li>Se você não se registrou, ignore este email</li>
                            </ul>
                        </div>
                        
                        <p><strong>Link alternativo:</strong></p>
                        <p style="word-break: break-all; font-size: 12px; color: ##666;">%s</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Order App. Todos os direitos reservados.</p>
                        <p>Este é um email automático, por favor não responda.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, confirmationUrl, confirmationUrl);
    }
    
    /**
     * Método genérico para enviar email HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            
            Transport.send(message);
            System.out.println("✅ Email de confirmação enviado para: " + to);
            
        } catch (MessagingException e) {
            System.err.println("❌ Erro ao enviar email: " + e.getMessage());
            throw new RuntimeException("Falha ao enviar email de confirmação", e);
        }
    }
    /**
     * Template HTML para email de confirmação bem-sucedida
     */
    private String buildEmailConfirmedBody(String userName) {
        //String loginUrl = "http://localhost:3000/login";
        String loginUrl = frontendUrl +"/login";
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: ##333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, ##10b981 0%%, ##059669 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: ##f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .success-icon { font-size: 60px; margin: 20px 0; }
                    .button { display: inline-block; padding: 15px 40px; background: ##10b981; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }
                    .button:hover { background: ##059669; }
                    .footer { text-align: center; margin-top: 30px; color: ##999; font-size: 12px; }
                    .info-box { background: ##dbeafe; border-left: 4px solid ##3b82f6; padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .features { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .feature-item { margin: 15px 0; padding-left: 30px; position: relative; }
                    .feature-item:before { content: "✓"; position: absolute; left: 0; color: ##10b981; font-weight: bold; font-size: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="success-icon">✅</div>
                        <h1>Conta Confirmada!</h1>
                    </div>
                    <div class="content">
                        <h2>Parabéns, %s!</h2>
                        <p>Sua conta no <strong>Order App</strong> foi <strong>ativada com sucesso</strong>! 🎉</p>
                        
                        <div class="info-box">
                            <strong>📧 Email verificado:</strong> Sua conta está pronta para uso!
                        </div>
                        
                        <p>Agora você tem acesso completo a todas as funcionalidades da plataforma:</p>
                        
                        <div class="features">
                            <div class="feature-item">Gerenciar seus pedidos e produtos</div>
                            <div class="feature-item">Acessar relatórios e análises</div>
                            <div class="feature-item">Configurar sua conta e preferências</div>
                            <div class="feature-item">Conectar-se com outros usuários</div>
                        </div>
                        
                        <p>Clique no botão abaixo para fazer login e começar a usar o Order App:</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">🚀 Fazer Login</a>
                        </div>
                        
                        <p style="margin-top: 30px; color: ##666; font-size: 14px;">
                            <strong>💡 Dica:</strong> Não se esqueça de completar seu perfil e explorar todas as funcionalidades disponíveis!
                        </p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Order App. Todos os direitos reservados.</p>
                        <p>Se você não criou esta conta, por favor ignore este email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, loginUrl);
    }
}