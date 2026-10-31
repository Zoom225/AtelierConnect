package com.atelierconnect.service.impl;

import com.atelierconnect.dto.request.ParticipantRequest;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.mapper.ParticipantMapper;
import com.atelierconnect.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantCreationService {

    private final ParticipantService membreService;
    private final ParticipantMapper membreMapper;

    public Participant create(ParticipantRequest request) {
        Participant membre = membreMapper.toEntity(request);
        return membreService.create(membre, request.getSiteId(), request.getPassword());
    }
}
