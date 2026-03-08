package com.example.backend.service.impl;

import com.example.backend.entitie.Client;
import com.example.backend.entitie.NewsLetter;
import com.example.backend.repository.NewsLetterRepository;
import com.example.backend.service.NewsLetterService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class NewsLetterServiceImpl implements NewsLetterService {

    @Autowired
    private NewsLetterRepository newsLetterRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendEmailToClients(Long newsLetterId) {
        NewsLetter nl = newsLetterRepository.findById(newsLetterId)
                .orElseThrow(() -> new RuntimeException("NewsLetter not found"));

        // Update send date
        nl.setDateEnvoi(LocalDate.now());
        newsLetterRepository.save(nl);

        List<Client> clients = nl.getClients();
        if (clients == null || clients.isEmpty()) return;

        for (Client client : clients) {
            String email = client.getEmail();
            if (email == null || email.isBlank()) continue;
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(email);
                helper.setSubject(nl.getTitre());
                helper.setText(nl.getContenu(), true);
                mailSender.send(mimeMessage);
            } catch (MessagingException e) {
                // Log and continue with next client
                System.err.println("Failed to send newsletter to " + email + ": " + e.getMessage());
            }
        }
    }
}
