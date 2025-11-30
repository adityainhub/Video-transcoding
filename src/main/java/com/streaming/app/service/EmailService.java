package com.streaming.app.service;

import com.streaming.app.dto.ContactForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.support:support@fluxmedia.in}")
    private String supportEmail;

    @Value("${app.email.from:noreply@fluxmedia.in}")
    private String fromEmail;

    public void sendContactFormEmail(ContactForm form) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(supportEmail);
            message.setSubject("New Contact Form Submission - fluxmedia");
            message.setFrom(fromEmail);
            message.setReplyTo(form.getEmail()); // User can reply directly

            String emailBody = String.format("""
                New contact form submission from fluxmedia website:
                
                Name: %s
                Email: %s
                
                Message:
                %s
                
                ---
                This is an automated message from fluxmedia contact form.
                """,
                    form.getName(),
                    form.getEmail(),
                    form.getMessage()
            );

            message.setText(emailBody);

            mailSender.send(message);
            System.out.println("Contact form email sent successfully from: " + form.getEmail());

        } catch (Exception e) {
            System.out.println("Failed to send contact form email from: " + form.getEmail());
            throw new RuntimeException("Failed to send email. Please try again later.", e);
        }
    }

    public void sendConfirmationEmail(ContactForm form) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(form.getEmail());
            message.setSubject("We received your message - fluxmedia");
            message.setFrom(fromEmail);

            String emailBody = String.format("""
                Hi %s,
                
                Thank you for contacting fluxmedia! We have received your message and will get back to you within 24-48 hours.
                
                Your message:
                %s
                
                Best regards,
                fluxmedia Team
                
                ---
                This is an automated confirmation email.
                """,
                    form.getName(),
                    form.getMessage()
            );

            message.setText(emailBody);

            mailSender.send(message);

            System.out.println("Confirmation email sent successfully to: " + form.getEmail());

        } catch (Exception e) {
            System.out.println("Failed to send confirmation email to: " + form.getEmail());
            // Don't throw exception - confirmation email is optional
        }
    }
}