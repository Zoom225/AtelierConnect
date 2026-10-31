package com.padell.padell.service.impl;

import com.padell.padell.dto.request.ParticipantRequest;
import com.padell.padell.entity.Participant;
import com.padell.padell.mapper.ParticipantMapper;
import com.padell.padell.service.ParticipantService;
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
