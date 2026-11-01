package com.atelierconnect.repository;

import com.atelierconnect.entity.JourFermeture;
import com.atelierconnect.entity.Centre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests de JourFermetureRepository")
class JourFermetureRepositoryTest {

    @Autowired
    private JourFermetureRepository jourFermetureRepository;

    @Autowired
    private CentreRepository centreRepository;

    @Test
    @DisplayName("existsByDateAndGlobalTrue détecte une fermeture globale")
    void shouldFindGlobalClosingDay() {
        LocalDate date = LocalDate.of(2026, 12, 25);
        jourFermetureRepository.save(JourFermeture.builder()
                .date(date)
                .raison("Noël")
                .global(true)
                .build());

        assertThat(jourFermetureRepository.existsByDateAndGlobalTrue(date)).isTrue();
    }

    @Test
    @DisplayName("existsByDateAndSiteId détecte une fermeture de centre")
    void shouldFindCentreClosingDay() {
        Centre centre = centreRepository.save(centre());
        LocalDate date = LocalDate.of(2026, 7, 14);
        jourFermetureRepository.save(JourFermeture.builder()
                .date(date)
                .raison("Maintenance")
                .global(false)
                .centre(centre)
                .build());

        assertThat(jourFermetureRepository.existsByDateAndCentreId(date, centre.getId())).isTrue();
    }

    private Centre centre() {
        return Centre.builder()
                .nom("Centre collaboratif Lyon")
                .adresse("12 rue de la République")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeAtelierMinutes(90)
                .dureeEntreAtelierMinutes(15)
                .anneeCivile(2026)
                .build();
    }
}
