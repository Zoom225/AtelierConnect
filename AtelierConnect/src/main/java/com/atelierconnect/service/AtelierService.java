package com.atelierconnect.service;

import com.atelierconnect.dto.request.AtelierRequest;
import com.atelierconnect.entity.Atelier;
import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.dto.response.AtelierDto;

import java.util.List;

public interface AtelierService {

    AtelierDto createAtelier(CreateAtelierRequest request, String username);

    List<AtelierDto> findAllAteliers();

    List<AtelierDto> getPublicAvailableAteliers();

    List<AtelierDto> findByOrganisateur(Long organisateurId);

    List<AtelierDto> findByCentre(Long centreId);

    Atelier getAtelierEntityById(Long id);

    AtelierDto getAtelierDtoById(Long id);

    AtelierDto updateAtelier(Long atelierId, AtelierRequest request);

    void cancelAtelier(Long atelierId, Long requesterId);

    void convertToPublic(Long atelierId);

    void consentToPublicOpening(Long atelierId, Long requesterId);

    void incrementParticipants(Long atelierId);

    void decrementParticipants(Long atelierId);

    void checkAndConvertExpiredPrivateAteliers();
}
