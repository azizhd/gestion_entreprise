package com.example.backend.service;

import com.example.backend.audit.ActionType;
import com.example.backend.audit.AuditAction;
import com.example.backend.dto.ClientDto;
import org.springframework.data.domain.Page;

public interface ClientService {
    Page<ClientDto> getClients(int page, int size);

    ClientDto getClient(Long id);

    @AuditAction(action = "CLIENT_CREATE", type = ActionType.CREATE, entityType = "Client")
    ClientDto createClient(ClientDto clientDto);

    @AuditAction(action = "CLIENT_UPDATE", type = ActionType.UPDATE, entityType = "Client")
    ClientDto updateClient(Long id, ClientDto clientDto);

    @AuditAction(action = "CLIENT_DELETE", type = ActionType.DELETE, entityType = "Client")
    void deleteClient(Long id);
}
