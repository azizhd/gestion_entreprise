package com.example.backend.service;

import com.example.backend.entitie.Devis;

public interface PdfService {
    byte[] generateDevisPdf(Devis devis) throws Exception;
}
