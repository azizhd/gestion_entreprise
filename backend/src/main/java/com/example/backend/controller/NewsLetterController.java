package com.example.backend.controller;

import com.example.backend.service.NewsLetterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/newsletters")
public class NewsLetterController {

    @Autowired
    private NewsLetterService newsLetterService;

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendToClients(@PathVariable Long id) {
        newsLetterService.sendEmailToClients(id);
        return ResponseEntity.accepted().build();
    }
}
