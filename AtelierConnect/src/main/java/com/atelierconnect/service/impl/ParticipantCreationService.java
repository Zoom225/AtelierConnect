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

    private final ParticipantService participantService;
    private final ParticipantMapper participantMapper;

    public Participant create(ParticipantRequest request) {
        Participant participant = participantMapper.toEntity(request);
        return participantService.create(participant, request.getCentreId(), request.getPassword());
    }
}
