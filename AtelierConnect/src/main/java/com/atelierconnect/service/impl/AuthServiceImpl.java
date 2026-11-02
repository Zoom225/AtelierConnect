package com.atelierconnect.service.impl;

import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.dto.request.LoginRequest;
import com.atelierconnect.dto.response.LoginResponse;
import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AdministrateurRepository administrateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    @Override
    public LoginResponse login(LoginRequest request) {
        Administrateur admin = administrateurRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Identifiants invalides."));

        // Regle metier : un administrateur doit fournir un mot de passe valide.
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException("Identifiants invalides.");
        }

        String token = jwtConfig.generateToken(
                admin.getEmail(),
                admin.getTypeAdministrateur().name(),
                "ADMIN"
        );

        log.info("Administrateur {} connecté avec succès", admin.getEmail());

        return LoginResponse.builder()
                .token(token)
                .email(admin.getEmail())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .role(admin.getTypeAdministrateur())
                .centreId(admin.getCentre() != null ? admin.getCentre().getId() : null)
                .build();
    }
}
