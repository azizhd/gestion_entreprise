package com.example.backend.repository;

import com.example.backend.entitie.NewsLetter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsLetterRepository extends JpaRepository<NewsLetter, Long> {
}
