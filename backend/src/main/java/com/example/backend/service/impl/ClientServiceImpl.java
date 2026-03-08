package com.example.backend.service.impl;

import com.example.backend.dto.ClientDto;
import com.example.backend.audit.AuditAction;
import com.example.backend.audit.ActionType;
import com.example.backend.entitie.Client;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.mapper.ClientMapper;
import com.example.backend.repository.ClientRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.ClientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;

    public ClientServiceImpl(ClientRepository clientRepository,
                             ClientMapper clientMapper,
                             UtilisateurRepository utilisateurRepository,
                             EntrepriseRepository entrepriseRepository) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public Page<ClientDto> getClients(int page, int size) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Page<Client> clients = clientRepository.findByEntreprise_Id(entreprise.getId(), PageRequest.of(page, size));
        return clients.map(clientMapper::toDTO);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public ClientDto getClient(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Client client = clientRepository.findByIdAndEntreprise_Id(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        return clientMapper.toDTO(client);
    }

    @Override
    @AuditAction(action = "CLIENT_CREATE", entityType = "Client", type = ActionType.CREATE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public ClientDto createClient(ClientDto clientDto) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Client client = clientMapper.toEntity(clientDto);
        client.setId(null);
        client.setEntreprise(entreprise);
        Client saved = clientRepository.save(client);
        return clientMapper.toDTO(saved);
    }

    @Override
    @AuditAction(action = "CLIENT_UPDATE", entityType = "Client", type = ActionType.UPDATE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    public ClientDto updateClient(Long id, ClientDto clientDto) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Client existing = clientRepository.findByIdAndEntreprise_Id(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        existing.setAdresse(clientDto.getAdresse());
        existing.setTelephone(clientDto.getTelephone());
        existing.setEmail(clientDto.getEmail());
        existing.setNom(clientDto.getNom());
        existing.setPaymentTermsDays(clientDto.getPaymentTermsDays());
        Client saved = clientRepository.save(existing);
        return clientMapper.toDTO(saved);
    }

    @Override
    @AuditAction(action = "CLIENT_DELETE", entityType = "Client", type = ActionType.DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteClient(Long id) {
        Entreprise entreprise = resolveCurrentEntreprise();
        Client existing = clientRepository.findByIdAndEntreprise_Id(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        clientRepository.delete(existing);
    }

    private Entreprise resolveCurrentEntreprise() {
        Utilisateur currentUser = resolveCurrentUser();
        return entrepriseRepository.findByManager_Id(currentUser.getId())
                .orElseGet(() -> {
                    if (currentUser.getEntreprise() == null) {
                        throw new SecurityException("Current user is not linked to an entreprise");
                    }
                    return entrepriseRepository.findById(currentUser.getEntreprise().getId())
                            .orElseThrow(() -> new SecurityException("Entreprise not found for current user"));
                });
    }

    private Utilisateur resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
    }
}
