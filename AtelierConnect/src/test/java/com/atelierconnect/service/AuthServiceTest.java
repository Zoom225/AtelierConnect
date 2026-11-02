package com.atelierconnect.service;

import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.dto.request.LoginRequest;
import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.entity.enums.TypeAdministrateur;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final AdministrateurRepository repository = mock(AdministrateurRepository.class);
    private final JwtConfig jwtConfig = mock(JwtConfig.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthServiceImpl service = new AuthServiceImpl(repository, encoder, jwtConfig);

    @Test
    void emailAndHashedMatriculeAuthenticateAdministrator() {
        Administrateur admin = Administrateur.builder()
                .email("admin@atelierconnect.example")
                .matricule("ADMIN001")
                .passwordHash(encoder.encode("ADMIN001"))
                .typeAdministrateur(TypeAdministrateur.GLOBAL)
                .build();
        when(repository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        LoginRequest request = new LoginRequest();
        request.setEmail(admin.getEmail());
        request.setPassword(admin.getMatricule());

        assertThat(service.login(request).getEmail()).isEqualTo(admin.getEmail());
        verify(jwtConfig).generateToken(admin.getEmail(), "GLOBAL", "ADMIN");
    }

    @Test
    void wrongPasswordCannotAuthenticateOrGenerateJwt() {
        Administrateur admin = Administrateur.builder()
                .passwordHash(encoder.encode("ADMIN001"))
                .build();
        when(repository.findByEmail("admin@atelierconnect.example")).thenReturn(Optional.of(admin));
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@atelierconnect.example");
        request.setPassword("incorrect");

        assertThatThrownBy(() -> service.login(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(jwtConfig);
    }
}
