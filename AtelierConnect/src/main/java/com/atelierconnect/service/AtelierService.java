package com.atelierconnect.service;

import com.atelierconnect.dto.request.AtelierRequest;
import com.atelierconnect.entity.Atelier;
import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.dto.response.AtelierDto;

import java.util.List;

public interface AtelierService {

    AtelierDto createMatch(CreateAtelierRequest request, String username);

    List<AtelierDto> findAllMatches();

    List<AtelierDto> getPublicAvailableMatches();

    List<AtelierDto> findByOrganisateur(Long organisateurId);

    List<AtelierDto> findBySite(Long siteId);

    Atelier getMatchEntityById(Long id);

    AtelierDto getMatchDtoById(Long id);

    AtelierDto updateMatch(Long matchId, AtelierRequest request);

    void cancelMatch(Long matchId, Long requesterId);

    void convertToPublic(Long matchId);

    void consentToPublicOpening(Long matchId, Long requesterId);

    void incrementPlayers(Long matchId);

    void decrementPlayers(Long matchId);

    void checkAndConvertExpiredPrivateAteliers();
}
