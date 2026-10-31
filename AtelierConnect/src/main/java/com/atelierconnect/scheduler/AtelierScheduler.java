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
        paiementService.checkUnpaidBeforeMatch();
        log.info("Vérification des réservations impayées terminée");
    }
}
