package com.atelierconnect.scheduler;

import com.atelierconnect.service.AtelierService;
import com.atelierconnect.service.PaiementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AtelierScheduler {

    private final AtelierService atelierService;
    private final PaiementService paiementService;

    // tourne tous les jours à minuit
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredPrivateAteliers() {
        log.info("Vérification des ateliers privés avec accord d'ouverture publique");
        atelierService.checkAndConvertExpiredPrivateAteliers();
        log.info("Vérification des ouvertures publiques terminée");
    }

    // tourne tous les jours à minuit cinq (après le premier)
    @Scheduled(cron = "0 5 0 * * *")
    public void processUnpaidReservations() {
        log.info("Vérification des réservations impayées avant les ateliers");
        paiementService.checkUnpaidBeforeAtelier();
        log.info("Vérification des réservations impayées terminée");
    }

    // Contrôle récurrent à J-1, notamment après un redémarrage ou une création
    // tardive. Le traitement sous verrou ne sanctionne chaque atelier qu'une fois.
    @Scheduled(cron = "${APP_PRIVATE_PENALTY_CRON:0 */5 * * * *}")
    public void processIncompletePrivateAteliers() {
        log.info("Vérification des ateliers privés incomplets à J-1");
        atelierService.checkAndPenalizeIncompletePrivateAteliers();
        log.info("Vérification des suspensions pour atelier privé incomplet terminée");
    }
}
