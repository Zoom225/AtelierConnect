package com.padell.padell.scheduler;

import com.padell.padell.service.AtelierService;
import com.padell.padell.service.PaiementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AtelierScheduler {

    private final AtelierService matchService;
    private final PaiementService paiementService;

    // tourne tous les jours à minuit
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredPrivateAteliers() {
        log.info("Vérification des ateliers privés avec accord d'ouverture publique");
        matchService.checkAndConvertExpiredPrivateMatches();
        log.info("Vérification des ouvertures publiques terminée");
    }

    // tourne tous les jours à minuit cinq (après le premier)
    @Scheduled(cron = "0 5 0 * * *")
    public void processUnpaidReservations() {
        log.info("Scheduler started : checking unpaid reservations...");
        paiementService.checkUnpaidBeforeMatch();
        log.info("Scheduler done : unpaid reservations processed");
    }
}
